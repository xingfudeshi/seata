/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.tm.api;


import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.core.exception.TransactionException;
import io.seata.tm.api.transaction.TransactionHook;
import io.seata.tm.api.transaction.TransactionHookManager;
import io.seata.tm.api.transaction.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Template of executing business logic with a global transaction.
 *
 * @author sharajava
 */
public class TransactionalTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalTemplate.class);


    /**
     * Execute object.
     *
     * @param business the business
     * @return the object
     * @throws TransactionalExecutor.ExecutionException the execution exception
     */
    public Object execute(TransactionalExecutor business) throws Throwable {
        // 1. get or create a transaction
        /**
         * 全局事务开始的时候,会创建一个新的全局事务,角色为:GlobalStatus.UnKnown, GlobalTransactionRole.Launcher
         * 可以参考这里 {@link DefaultGlobalTransaction#DefaultGlobalTransaction()}
         *
         * 如果是作为一个全局事务参与者,那么就会根据现有的xid,封装一个GlobalTransaction,角色为:GlobalStatus.Begin, GlobalTransactionRole.Participant
         * 可以参考这里{@link GlobalTransactionContext#getCurrent()}
         */
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        // 1.1 get transactionInfo
        TransactionInfo txInfo = business.getTransactionInfo();
        if (txInfo == null) {
            throw new ShouldNeverHappenException("transactionInfo does not exist");
        }
        try {

            // 2. begin transaction
            /**
             * //开始执行事务的操作,实际上调用的是:{@link DefaultGlobalTransaction#begin(int, String)}
             */
            beginTransaction(txInfo, tx);

            Object rs = null;
            try {

                // Do Your Business
                /**
                 * 执行真正的业务逻辑,这里会调用MethodInterceptor#invoke传入进来的methodInvocation.proceed();
                 */
                rs = business.execute();

            } catch (Throwable ex) {

                // 3.the needed business exception to rollback.
                /**
                 * 报异常,需要回滚
                 */
                completeTransactionAfterThrowing(txInfo,tx,ex);
                throw ex;
            }
            /**
             * 没有异常,则提交
             * 调用的是{@link DefaultGlobalTransaction#commit()} (int, String)}
             */
            // 4. everything is fine, commit.
            commitTransaction(tx);

            return rs;
        } finally {
            //5. clear
            triggerAfterCompletion();
            cleanUp();
        }
    }

    private void completeTransactionAfterThrowing(TransactionInfo txInfo, GlobalTransaction tx, Throwable ex) throws TransactionalExecutor.ExecutionException {
        //roll back
        if (txInfo != null && txInfo.rollbackOn(ex)) {
            try {
                rollbackTransaction(tx, ex);
            } catch (TransactionException txe) {
                // Failed to rollback
                throw new TransactionalExecutor.ExecutionException(tx, txe,
                        TransactionalExecutor.Code.RollbackFailure, ex);
            }
        } else {
            // not roll back on this exception, so commit
            commitTransaction(tx);
        }
    }

    private void commitTransaction(GlobalTransaction tx) throws TransactionalExecutor.ExecutionException {
        try {
            triggerBeforeCommit();
            tx.commit();
            triggerAfterCommit();
        } catch (TransactionException txe) {
            // 4.1 Failed to commit
            throw new TransactionalExecutor.ExecutionException(tx, txe,
                TransactionalExecutor.Code.CommitFailure);
        }
    }

    private void rollbackTransaction(GlobalTransaction tx, Throwable ex) throws TransactionException, TransactionalExecutor.ExecutionException {
        triggerBeforeRollback();
        tx.rollback();
        triggerAfterRollback();
        // 3.1 Successfully rolled back
        throw new TransactionalExecutor.ExecutionException(tx, TransactionalExecutor.Code.RollbackDone, ex);
    }

    private void beginTransaction(TransactionInfo txInfo, GlobalTransaction tx) throws TransactionalExecutor.ExecutionException {
        try {
            triggerBeforeBegin();
            tx.begin(txInfo.getTimeOut(), txInfo.getName());
            triggerAfterBegin();
        } catch (TransactionException txe) {
            throw new TransactionalExecutor.ExecutionException(tx, txe,
                TransactionalExecutor.Code.BeginFailure);

        }
    }

    private void triggerBeforeBegin() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeBegin();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeBegin in hook " + e.getMessage());
            }
        }
    }

    private void triggerAfterBegin() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterBegin();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterBegin in hook " + e.getMessage());
            }
        }
    }

    private void triggerBeforeRollback() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeRollback();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeRollback in hook " + e.getMessage());
            }
        }
    }

    private void triggerAfterRollback() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterRollback();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterRollback in hook " + e.getMessage());
            }
        }
    }

    private void triggerBeforeCommit() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeCommit();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeCommit in hook " + e.getMessage());
            }
        }
    }

    private void triggerAfterCommit() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterCommit();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterCommit in hook " + e.getMessage());
            }
        }
    }

    private void triggerAfterCompletion() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterCompletion();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterCompletion in hook " + e.getMessage());
            }
        }
    }

    private void cleanUp() {
        TransactionHookManager.clear();
    }

    private List<TransactionHook> getCurrentHooks() {
        return TransactionHookManager.getHooks();
    }

}
