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

import com.cowave.commons.framework.helper.redis.StringRedisHelper;
import lombok.RequiredArgsConstructor;

import java.util.Collections;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
public class RedisAccessLimiter implements AccessLimiter {

    // 以微秒为单位进行限制
    public static final String LUA_SCRIPT = """
            local key = KEYS[1]     
            local limit = tonumber(ARGV[1])  
            local window = tonumber(ARGV[2])  
            local nowData = redis.call("TIME")
            local now = (nowData[1] * 1000000) + nowData[2]            
            redis.call("ZREMRANGEBYSCORE", key, 0, now - window * 1000)
            
            local currentCount = redis.call("ZCARD", key)
            if currentCount >= limit then
               return 0
            else
               redis.call("ZADD", key, now, now)
               redis.call("PEXPIRE", key, window)
               return 1
            end
            """;

    private final StringRedisHelper stringRedisHelper;

    @Override
    public boolean throughLimit(String limitKey, long period, long limits) {
        return stringRedisHelper.luaExec(LUA_SCRIPT, Long.class,
                Collections.singletonList(limitKey), String.valueOf(limits), String.valueOf(period)) > 0;
    }
}
