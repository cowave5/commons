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
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
public class RedisCaffeineCache extends AbstractValueAdaptingCache {
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final String cacheName;
    private final CacheProperties cacheProperties;
    private final ApplicationProperties applicationProperties;
    private final Cache<Object, Object> local;
    private final RedisHelper redisHelper;
    private final StringRedisHelper stringRedisHelper;

    protected RedisCaffeineCache(String cacheName, CacheProperties cacheProperties,
                                 ApplicationProperties applicationProperties, Cache<Object, Object> local,
                                 RedisHelper redisHelper, StringRedisHelper stringRedisHelper) {
        super(true);
        this.cacheName = cacheName;
        this.cacheProperties = cacheProperties;
        this.applicationProperties = applicationProperties;
        this.local = local;
        this.redisHelper = redisHelper;
        this.stringRedisHelper = stringRedisHelper;
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = lookup(key);
        if (value != null) {
            return (T) value;
        }

        ReentrantLock lock = locks.get(key.toString());
        if (lock == null) {
            lock = locks.computeIfAbsent(key.toString(), k -> new ReentrantLock());
        }
        try {
            lock.lock();
            value = lookup(key);
            if (value != null) {
                return (T) value;
            }

            value = valueLoader.call();
            Object storeValue = toStoreValue(value);
            put(key, storeValue);
            return (T) value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e.getCause());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected Object lookup(Object key) {
        if(!cacheProperties.l1Enable() && !cacheProperties.l2Enable(cacheName)){
            return null; // 不缓存
        }

        if (cacheProperties.l1Enable() && cacheProperties.l2Enable(cacheName)) {
            // 启用L1和L2缓存
            if (cacheProperties.l2First(cacheName)) {
                // L2优先
                return l2FirstLookup(key);
            } else {
                // L1优先
                return l1FirstLookup(key);
            }
        } else if (cacheProperties.l1Enable()) {
            // 启用L1缓存
            return localGet(key);
        } else {
            // 启用L2缓存
            return redisGet(key);
        }
    }

    private Object l2FirstLookup(Object key) {
        // L2获取
        Object value = redisGet(key);
        if (value != null) {
            return value;
        }
        // L1获取
        value = localGet(key);
        // L1同步到L2
        if (value != null) {
            redisPut(key, value);
        }
        return value;
    }

    private Object l1FirstLookup(Object key) {
        // L1获取
        Object value = localGet(key);
        if (value != null) {
            return value;
        }
        // L2获取
        value = redisGet(key);
        // L2同步到L1
        if (value != null) {
            localPut(key, toStoreValue(value));
        }
        return value;
    }

    private Object localGet(Object key){
        Object value = local.getIfPresent(key);
        if(cacheProperties.isLogEnable()){
            log.info("L1-cache get, key={}, value={}", key, value);
        }
        return value;
    }

    private void localPut(Object key, Object value){
        local.put(key, toStoreValue(value));
        if(cacheProperties.isLogEnable()){
            log.info("L1-cache put, key={}, value={}", key, value);
        }
    }

    private Object redisGet(Object key){
        String redisKey = redisKey(key);
        Object value = null;
        try{
            value = redisHelper.getValue(redisKey);
            if(value != null){
                redisHelper.expire(redisKey, cacheProperties.l2ExpireAfterAccess(cacheName), TimeUnit.SECONDS);
            }
            if(cacheProperties.isLogEnable()){
                log.info("L2-cache get, key={}, value={}", redisKey, value);
            }
        }catch (Exception e){
            log.error("L2-cache get failed, key={}", redisKey, e);
        }
        return value;
    }

    private void redisPut(Object key, Object value){
        String redisKey = redisKey(key);
        int expire = cacheProperties.l2ExpireAfterWrite(cacheName);
        try{
            if (expire > 0) {
                redisHelper.putExpire(redisKey, toStoreValue(value), expire, TimeUnit.SECONDS);
            } else {
                redisHelper.putValue(redisKey, toStoreValue(value));
            }
            if(cacheProperties.isLogEnable()){
                log.info("L2-cache put, key={}, value={}", redisKey, value);
            }
        }catch(Exception e){
            log.error("L2-cache put failed, key={}", redisKey, e);
        }
    }

    private String redisKey(Object key) {
        return applicationProperties.getName() + ":cache:" + cacheName + ":" + key.toString();
    }

    @Override
    public void put(Object key, Object value) {
        if(value == null){
          return;
        }
        if (cacheProperties.l1Enable()){
            localPut(key, value);
        }
        if(cacheProperties.l2Enable(cacheName)){
            redisPut(key, value);
        }
    }

    @Override
    public void evict(Object key) {
        if(cacheProperties.l2Enable(cacheName)){
            redisHelper.delete(redisKey(key));
        }
        local.invalidate(key);
    }

    @Override
    public void clear() {
        stringRedisHelper.luaClean(this.cacheName.concat(":*"));
        local.invalidateAll();
    }
}
