/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.limiter;

import com.google.common.util.concurrent.RateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author aKuang
 */
public class MethodLimiterHelper {

    private static final Map<String, RateLimiter> RATE_LIMITER = new ConcurrentHashMap<>();

    static RateLimiter getLimiter(String key, double permitsPerSecond) {
        RateLimiter limiter = RATE_LIMITER.get(key);
        if (limiter != null) {
            return limiter;
        }

        limiter = RateLimiter.create(permitsPerSecond);
        RateLimiter currentLimiter = RATE_LIMITER.putIfAbsent(key, limiter);
        return currentLimiter == null ? limiter : currentLimiter;
    }

    public static void acquire(String name, double permitsPerSecond) {
        MethodLimiterHelper.getLimiter(name, permitsPerSecond).acquire();
    }

    public static boolean tryAcquire(String name, double permitsPerSecond) {
        return MethodLimiterHelper.getLimiter(name, permitsPerSecond).tryAcquire();
    }

    public static boolean tryAcquire(String name, double permitsPerSecond, long waitTime, TimeUnit timeUnit) {
        return MethodLimiterHelper.getLimiter(name, permitsPerSecond).tryAcquire(waitTime, timeUnit);
    }
}
