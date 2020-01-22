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
package io.seata.spring.boot.autoconfigure.properties.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = CLIENT_PREFIX)
public class ClientProperties {
    private int asyncCommitBufferLimit = 10000;
    private int reportRetryCount = 5;
    private int commitRetryCount = 5;
    private int rollbackRetryCount = 5;
    private boolean tableMetaCheckEnable = false;
    private boolean reportSuccessEnable = true;

    public int getAsyncCommitBufferLimit() {
        return asyncCommitBufferLimit;
    }

    public ClientProperties setAsyncCommitBufferLimit(int asyncCommitBufferLimit) {
        this.asyncCommitBufferLimit = asyncCommitBufferLimit;
        return this;
    }

    public int getReportRetryCount() {
        return reportRetryCount;
    }

    public ClientProperties setReportRetryCount(int reportRetryCount) {
        this.reportRetryCount = reportRetryCount;
        return this;
    }

    public int getCommitRetryCount() {
        return commitRetryCount;
    }

    public ClientProperties setCommitRetryCount(int commitRetryCount) {
        this.commitRetryCount = commitRetryCount;
        return this;
    }

    public int getRollbackRetryCount() {
        return rollbackRetryCount;
    }

    public ClientProperties setRollbackRetryCount(int rollbackRetryCount) {
        this.rollbackRetryCount = rollbackRetryCount;
        return this;
    }

    public boolean isTableMetaCheckEnable() {
        return tableMetaCheckEnable;
    }

    public ClientProperties setTableMetaCheckEnable(boolean tableMetaCheckEnable) {
        this.tableMetaCheckEnable = tableMetaCheckEnable;
        return this;
    }

    public boolean isReportSuccessEnable() {
        return reportSuccessEnable;
    }

    public ClientProperties setReportSuccessEnable(boolean reportSuccessEnable) {
        this.reportSuccessEnable = reportSuccessEnable;
        return this;
    }
}
