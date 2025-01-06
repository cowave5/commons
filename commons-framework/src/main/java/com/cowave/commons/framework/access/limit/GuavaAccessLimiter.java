/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.limit;

import com.google.common.util.concurrent.RateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author shanhuiming
 *
 */
public class GuavaAccessLimiter implements AccessLimiter {

    private final Map<String, RateLimiter> localLimiter = new ConcurrentHashMap<>();

    @Override
    public boolean throughLimit(String limitKey, long period, long limits) {
        return getLimiter(limitKey, period, limits).tryAcquire();
    }

    private RateLimiter getLimiter(String limitKey, long period, long limits) {
        String key = limitKey + "_" + period + "_" + limits;
        RateLimiter limiter = localLimiter.get(key);
        if (limiter != null) {
            return limiter;
        }
        // RateLimiter的令牌数只能以秒为单位发放，简单换算下
        double permitsPerSecond = limits * 1000.0 / period;
        return localLimiter.computeIfAbsent(key, k -> RateLimiter.create(permitsPerSecond));
    }
}
