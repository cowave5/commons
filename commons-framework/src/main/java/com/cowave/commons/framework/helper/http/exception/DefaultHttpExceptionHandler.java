/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.http.exception;

import com.cowave.commons.client.http.HttpExceptionHandler;
import com.cowave.commons.client.http.asserts.HttpException;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.tm.api.GlobalTransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
public class DefaultHttpExceptionHandler implements HttpExceptionHandler {

    @Override
    public void handle(HttpException e) {
        String xid = RootContext.getXID();
        if(StringUtils.isNotBlank(xid)){
            try {
                GlobalTransactionContext.reload(xid).rollback();
            } catch (TransactionException ex) {
                log.error("Rollback failed[" + xid + "]", ex);
            }
        }
    }
}
