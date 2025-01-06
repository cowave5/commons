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

import com.cowave.commons.client.http.asserts.AssertsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 *
 * @author aKuang
 *
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CallerLimitAspect {

    @Pointcut("@annotation(com.cowave.commons.framework.support.limit.CallerLimit)")
    public void pointCut() {

    }

    @Around("pointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        CallerLimit limit = signature.getMethod().getAnnotation(CallerLimit.class);
        String name = limit.name();
        if (StringUtils.isBlank(name)) {
            String className = Optional.ofNullable(point.getTarget()).map(Object::getClass).map(Class::getSimpleName).orElse("");
            String methodName = Optional.ofNullable(signature.getMethod()).map(Method::getName).orElse("");
            name = className + "_" + methodName;
        }

        long waitTime = limit.waitTime();
        if (waitTime == -1) {
            LimitHelper.acquire(name, limit.permitsPerSecond());
        } else if (!LimitHelper.tryAcquire(name, limit.permitsPerSecond(), waitTime, limit.timeUnit())) {
            throw new AssertsException("{frame.caller.limit}");
        }
        return point.proceed();
    }
}
