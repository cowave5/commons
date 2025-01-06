/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.lambda.convert;

import com.cowave.commons.tools.Converts;
import com.cowave.commons.tools.ReflectUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jiangbo
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ConverterCache {

    private static final Object LOCK = new Object();

    private static final Map<Class<?>, Class<?>> CACHE = new HashMap<>();

    public static <T> Class<T> getCacheClass(Class<? extends Converter<T>> clazz) {
        Class<?> targetClass = CACHE.get(clazz);
        if (targetClass == null) {
            synchronized (LOCK) {
                targetClass = CACHE.get(clazz);
                if (targetClass == null) {
                    targetClass = getTargetClass(clazz);
                }
            }
        }
        return Converts.cast(targetClass);
    }

    private static <T> Class<T> getTargetClass(Class<? extends Converter<T>> clazz) {
        Type type = ReflectUtils.getGenericInterfaceType(clazz, Converter.class);
        type = type instanceof ParameterizedType parameterizedType ? parameterizedType.getRawType() : type;
        Class<T> targetClass = Converts.cast(type);
        CACHE.put(clazz, targetClass);
        return targetClass;
    }
}
