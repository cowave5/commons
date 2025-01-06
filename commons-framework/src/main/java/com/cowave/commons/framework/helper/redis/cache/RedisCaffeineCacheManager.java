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
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
public class RedisCaffeineCacheManager implements CacheManager {

    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    private final CacheProperties cacheProperties;

    private final ApplicationProperties applicationProperties;

    private final RedisHelper redisHelper;

    private final StringRedisHelper stringRedisHelper;

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }

    @Override
    public Cache getCache(String cacheName) {
        Cache cache = caches.get(cacheName);
        if(cache != null) {
            return cache;
        }
        return caches.computeIfAbsent(cacheName,
                k -> new RedisCaffeineCache(cacheName, cacheProperties,
                        applicationProperties, caffeine(), redisHelper, stringRedisHelper));
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeine(){
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder();
        if(cacheProperties.l1ExpireAfterAccess() > 0){
            caffeineBuilder.expireAfterAccess(cacheProperties.l1ExpireAfterAccess(), TimeUnit.SECONDS);
        }
        if(cacheProperties.l1ExpireAfterWrite() > 0){
            caffeineBuilder.expireAfterWrite(cacheProperties.l1ExpireAfterWrite(), TimeUnit.SECONDS);
        }
        if(cacheProperties.l1InitialCapacity() > 0){
            caffeineBuilder.initialCapacity(cacheProperties.l1InitialCapacity());
        }
        if(cacheProperties.l1MaximumSize() > 0){
            caffeineBuilder.maximumSize(cacheProperties.l1MaximumSize());
        }
        if(cacheProperties.l1RefreshAfterWrite() > 0){
            caffeineBuilder.refreshAfterWrite(cacheProperties.l1RefreshAfterWrite(), TimeUnit.SECONDS);
        }
        return caffeineBuilder.build();
    }
}
