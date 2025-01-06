/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.operation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author shanhuiming
 *
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Operation {

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型
     */
    String type() default "";

    /**
     * 操作动作
     */
    String action() default "";

    /**
     * 操作描述，支持SPEL，可用参数
     * <p> 1.方法入参
     * <p> 2.resp: 返回值
     * <p> 3.exception: 异常对象
     */
    String desc() default "";

    /**
     * 操作处理，支持SPEL，可用参数：
     * <p> 1.opHandler: 处理类
     * <p> 2.方法入参
     * <p> 3.resp: 返回值
     * <p> 4.exception: 异常对象
     * <p> 5.opInfo: 操作信息（类型 OperationInfo）
     * <p>
     * <p> 示例：xxxHandler.doSomething(opInfo, resp, exception, ...)
     */
    String expr() default "";

    /**
     * 处理标识
     */
    String flag() default "";

    /**
     * 是否异步处理
     */
    boolean isAsync() default false;
}
