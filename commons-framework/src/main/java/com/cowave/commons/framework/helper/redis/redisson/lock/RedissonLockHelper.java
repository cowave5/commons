/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis.redisson.lock;

import com.cowave.commons.framework.configuration.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author aKuang
 *
 */
@Slf4j
@RequiredArgsConstructor
public class RedissonLockHelper {

    // 进程为单位，未区分线程
    private final Map<String, RLock> lockMap = new ConcurrentHashMap<>();

    private final RedissonClient redisson;

    private final ApplicationProperties applicationProperties;

    /**
     * 永久加锁
     * @param awaitTime 等待时间
     * @param timeUnit  时间单位
     * @param name  锁名称
     * @param keys  锁标识信息
     */
    public boolean tryLock(long awaitTime, TimeUnit timeUnit, String name, String... keys) {
        return tryLock(awaitTime, -1, timeUnit, name, keys);
    }

    /**
     * 加锁
     * @param awaitTime 等待时间
     * @param leaseTime 存续时间（-1表示永久）
     * @param timeUnit  时间单位
     * @param name  锁名称
     * @param keys  锁标识信息
     */
    public boolean tryLock(long awaitTime, long leaseTime, TimeUnit timeUnit, String name, String... keys) {
        String lockKey = key(name, keys);
        if(lockMap.containsKey(lockKey)){
            return true;
        }

        try {
            RLock lock = redisson.getLock(lockKey);
            boolean locked = lock.tryLock(awaitTime, leaseTime, timeUnit);
            if (locked) {
                log.debug("(redisson) lock success, key={}", lockKey);
                if(leaseTime == -1L){
                    lockMap.put(lockKey, lock);
                }
            }else{
                log.debug("(redisson) lock failed, key={}", lockKey);
            }
            return locked;
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    /**
     * 释放锁
     */
    public void releaseLock(String name, String... keys) {
        RLock rLock = lockMap.remove(key(name, keys));
        if (rLock != null) {
            rLock.forceUnlock();
        }
    }

    private String key(String name, String... keys) {
        if(keys != null && keys.length > 0){
            return applicationProperties.getLockNamespace() + name + ":" + String.join(":", keys);
        }
        return applicationProperties.getLockNamespace() + name;
    }
}
