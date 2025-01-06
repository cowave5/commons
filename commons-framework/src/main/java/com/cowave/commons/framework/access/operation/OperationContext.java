/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.operation;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
public class OperationContext {

    private static final ThreadLocal<OperationInfo> HOLDER = new TransmittableThreadLocal<>();

    public static void prepareContent(Object content){
        OperationInfo operationInfo = HOLDER.get();
        if(operationInfo == null){
            log.warn("operation not prepared");
            return;
        }
        operationInfo.setOpContent(content);
    }

    static void set(OperationInfo operationInfo){
        HOLDER.set(operationInfo);
    }

    static OperationInfo get(){
        return HOLDER.get();
    }

    static void remove(){
        HOLDER.remove();
    }
}
