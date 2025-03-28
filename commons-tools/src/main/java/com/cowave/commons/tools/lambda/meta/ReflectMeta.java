/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.lambda.meta;

import com.cowave.commons.tools.ReflectUtils;
import com.cowave.commons.tools.lambda.LambdaMeta;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * @author jiangbo
 */
@Slf4j
public class ReflectMeta implements LambdaMeta {

    private static final String SEMICOLON = ";";
    private static final String DOT = ".";
    private static final String SLASH = "/";
    private static final Field FIELD_CAPTURING_CLASS;

    static {
        Field fieldCapturingClass;
        try {
            Class<SerializedMeta> aClass = SerializedMeta.class;
            fieldCapturingClass = ReflectUtils.setAccessible(aClass.getDeclaredField("capturingClass"));
        } catch (Exception e) {
            // 解决高版本 jdk 的问题 gitee: https://gitee.com/baomidou/mybatis-plus/issues/I4A7I5
            log.warn(e.getMessage());
            fieldCapturingClass = null;
        }
        FIELD_CAPTURING_CLASS = fieldCapturingClass;
    }

    private final SerializedMeta lambda;

    public ReflectMeta(SerializedMeta lambda) {
        this.lambda = lambda;
    }

    @Override
    public String getMethodName() {
        return lambda.getImplMethodName();
    }

    @Override
    public Class<?> getInstantiatedClass() {
        String instantiatedMethodType = lambda.getInstantiatedMethodType();
        String instantiatedType = instantiatedMethodType.substring(2, instantiatedMethodType.indexOf(SEMICOLON)).replace(SLASH, DOT);
        return ReflectUtils.loadClass(instantiatedType, getCapturingClassClassLoader());
    }

    private ClassLoader getCapturingClassClassLoader() {
        // 如果反射失败，使用默认的 classloader
        if (FIELD_CAPTURING_CLASS == null) {
            return null;
        }

        try {
            return ((Class<?>) FIELD_CAPTURING_CLASS.get(lambda)).getClassLoader();
        } catch (IllegalAccessException e) {
            throw new UnsupportedOperationException(e);
        }
    }

}
