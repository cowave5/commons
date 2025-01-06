/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.limit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 *
 * @author aKuang
 *
 */
public class LimitLogger {

    public static void info(double secondRate, String format, Object... arguments) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 3) {
            StackTraceElement caller = stackTrace[2];
            String callerClass = caller.getClassName();
            String callerMethod = caller.getMethodName();
            if (LimitHelper.tryAcquire(callerClass + "." + callerMethod, secondRate)) {
                Logger logger = LoggerFactory.getLogger(callerClass);
                if (logger instanceof LocationAwareLogger locationLogger) {
                    locationLogger.log(null, LimitLogger.class.getName(), LocationAwareLogger.INFO_INT, format, arguments, null);
                } else {
                    logger.info(format, arguments);
                }
            }
        }
    }

    public static void error(double secondRate, String format, Object... arguments) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 2) {
            StackTraceElement caller = stackTrace[2];
            String callerClass = caller.getClassName();
            String callerMethod = caller.getMethodName();
            if (LimitHelper.tryAcquire(callerClass + "." + callerMethod, secondRate)) {
                Logger logger = LoggerFactory.getLogger(callerClass);
                if (logger instanceof LocationAwareLogger locationLogger) {
                    locationLogger.log(null, LimitLogger.class.getName(), LocationAwareLogger.ERROR_INT, format, arguments, null);
                }else{
                    logger.error(format, arguments);
                }
            }
        }
    }

    public static void error(double secondRate, Throwable t, String format, Object... arguments) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 2) {
            StackTraceElement caller = stackTrace[2];
            String callerClass = caller.getClassName();
            String callerMethod = caller.getMethodName();
            if (LimitHelper.tryAcquire(callerClass + "." + callerMethod, secondRate)) {
                Logger logger = LoggerFactory.getLogger(callerClass);
                if (logger instanceof LocationAwareLogger locationLogger) {
                    locationLogger.log(null, LimitLogger.class.getName(), LocationAwareLogger.ERROR_INT, format, arguments, t);
                }else{
                    logger.error(format, t, arguments);
                }
            }
        }
    }
}
