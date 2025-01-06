/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.configuration;

import com.alibaba.ttl.TtlRunnable;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
public class AsyncTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        return TtlRunnable.get(() -> {
            if(mdcMap != null){
                MDC.setContextMap(mdcMap);
            }else {
                MDC.clear(); // 避免线程复用导致问题
            }
            runnable.run();
        }, true, true);
    }
}
