/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.limit;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author aKuang
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CallerLimit {

    /**
     * 默认（类名 + 方法名）
     */
    String name() default "";

    /**
     * 每秒次数限制
     */
    double permitsPerSecond();

    /**
     * 等待时间（-1表示一直阻塞）
     */
    long waitTime() default 0;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}