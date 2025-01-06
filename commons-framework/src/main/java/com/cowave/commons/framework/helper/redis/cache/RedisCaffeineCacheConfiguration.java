/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis.cache;

import com.cowave.commons.framework.configuration.ApplicationProperties;
import com.cowave.commons.framework.helper.redis.RedisHelper;
import com.cowave.commons.framework.helper.redis.StringRedisHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;

/**
 *
 * @author shanhuiming
 *
 */
@EnableCaching
@RequiredArgsConstructor
@ConditionalOnClass(name = {"com.github.benmanes.caffeine.cache.Cache", "org.springframework.data.redis.core.RedisTemplate"})
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class RedisCaffeineCacheConfiguration {

    private final CacheProperties cacheProperties;

    private final ApplicationProperties applicationProperties;

    @Nullable
    private final RedisHelper redisHelper;

    @Nullable
    private final StringRedisHelper stringRedisHelper;

    @ConditionalOnMissingBean(CacheManager.class)
    @Bean
    public RedisCaffeineCacheManager cacheManager() {
        return new RedisCaffeineCacheManager(cacheProperties, applicationProperties, redisHelper, stringRedisHelper);
    }
}
