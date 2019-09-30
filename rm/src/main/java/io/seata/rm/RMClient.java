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
package io.seata.rm;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.model.Resource;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import io.seata.core.rpc.ClientMessageSender;
import io.seata.core.rpc.netty.AbstractRpcRemotingClient;
import io.seata.core.rpc.netty.RmMessageListener;
import io.seata.core.rpc.netty.RmRpcClient;

/**
 * The Rm client Initiator.
 *
 * @author jimin.jm @alibaba-inc.com
 */
public class RMClient {

    /**
     * Init.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     */
    public static void init(String applicationId, String transactionServiceGroup) {
        RmRpcClient rmRpcClient = RmRpcClient.getInstance(applicationId, transactionServiceGroup);
        /**
         * 初始化资源管理器
         * 这里有个概念和下面的RMHandler是一样,ResourceManager接口也有两个实现类,一个是{@link AbstractResourceManager},这个
         * 抽象类又被DataSourceManager所继承.另外一个ResourceManager的直接实现类就是{@link DefaultResourceManager},它的作用
         * 主要就是通过SPI来加载DataSourceManager.
         * 所以,系统中使用的时候,都是直接使用{@link DefaultResourceManager#get()}
         * 而下面的Handler使用的时候也是{@link DefaultRMHandler#get()}
         *
         * 在DataSourceProxy初始化的时候,就会把自身注册到DefaultResourceManager{@link DefaultResourceManager#registerResource(Resource)},注意,此时真正注册的是DataSourceManager,因为
         * SPI此时加载的resourceManager实际上是DataSourceManager,{@link DataSourceManager#registerResource}
         * {@link DataSourceProxy#init}
         */
        rmRpcClient.setResourceManager(DefaultResourceManager.get());
        /**
         * 在收到TC的回复消息后
         * 在这个地方会回调
         * {@link AbstractRpcRemotingClient#dispatch(RpcMessage, ChannelHandlerContext)}
         * 然后回调{@link RmMessageListener#onMessage(RpcMessage, String, ClientMessageSender)}
         * 注意RmMessageListener的构造方法传入进去的DefaultRMHandler.get(),此时会返回一个{@link AbstractRMHandler},实际上返回的是DefaultRMHandler
         * 然后在{@link DefaultRMHandler#initRMHandlers}会利用SPI加载/META-INF/services/io.seata.rm.AbstractRMHandler中的类
         * 这里实际上是RMHandlerAT,需要说明的是,虽然RMHandlerAT也是AbstractRMHandler的子类,但此时DefaultRMHandler.get()并不是直接返回的
         * RMHandlerAT,而是把它放进了一个allRMHandlersMap中以备后用,后续会通过一系列的handle返回来真正返回RMHandlerAT实例,可以
         * 查看{@link DefaultRMHandler#handle(BranchCommitRequest)},
         * {@link DefaultRMHandler#handle(BranchRollbackRequest)}
         * {@link DefaultRMHandler#handle(UndoLogDeleteRequest)}
         */
        rmRpcClient.setClientMessageListener(new RmMessageListener(DefaultRMHandler.get()));
        rmRpcClient.init();
    }

}
