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

import com.cowave.commons.framework.helper.redis.RedisAutoConfiguration;
import com.cowave.commons.framework.helper.redis.StringRedisHelper;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisOperations;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnClass(RedisOperations.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisAccessLimiterConfiguration {

    @ConditionalOnBean(StringRedisHelper.class)
    @ConditionalOnMissingBean(AccessLimiter.class)
    @Bean
    public AccessLimiter accessLimiter(StringRedisHelper stringRedisHelper){
        return new RedisAccessLimiter(stringRedisHelper);
    }
}
