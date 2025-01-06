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

import com.google.common.util.concurrent.RateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author aKuang
 */
public class LimitHelper {

    private static final Map<String, RateLimiter> LIMITERS = new ConcurrentHashMap<>();

    static RateLimiter getLimiter(String key, double permitsPerSecond) {
        RateLimiter limiter = LIMITERS.get(key);
        if (limiter != null) {
            return limiter;
        }
        return LIMITERS.computeIfAbsent(key, k -> RateLimiter.create(permitsPerSecond));
    }

    public static void acquire(String name, double permitsPerSecond) {
        LimitHelper.getLimiter(name, permitsPerSecond).acquire();
    }

    public static boolean tryAcquire(String name, double permitsPerSecond) {
        return LimitHelper.getLimiter(name, permitsPerSecond).tryAcquire();
    }

    public static boolean tryAcquire(String name, double permitsPerSecond, long waitTime, TimeUnit timeUnit) {
        return getLimiter(name, permitsPerSecond).tryAcquire(waitTime, timeUnit);
    }
}
