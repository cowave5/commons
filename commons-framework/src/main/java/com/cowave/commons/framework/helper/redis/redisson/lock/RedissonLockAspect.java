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

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

/**
 *
 * @author aKuang
 *
 */
@Aspect
@RequiredArgsConstructor
public class RedissonLockAspect {

    private final RedissonLockHelper lockHelper;

    @Pointcut("@annotation(com.cowave.commons.framework.helper.redis.redisson.lock.RedissonLock)")
    public void pointCut() {
    }

    @Around("pointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        RedissonLock redissonLock = signature.getMethod().getAnnotation(RedissonLock.class);
        String lockName = redissonLock.name();
        String[] lockKeys = redissonLock.keys();
        long awaitTime = redissonLock.await();
        long leaseTime = redissonLock.lease();
        try{
            if(lockHelper.tryLock(awaitTime, leaseTime, redissonLock.timeUnit(), lockName, lockKeys)){
                return point.proceed();
            }
        }finally {
            if(leaseTime != -1){
                lockHelper.releaseLock(lockName, lockKeys);
            }
        }
        return null;
    }
}
