/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.lambda;

import cn.hutool.core.util.ReflectUtil;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author jiangbo
 */
@FunctionalInterface
public interface GetterFunction<T, R> extends Function<T, R>, Serializable {

    /**
     * T 的类型
     */
    @SuppressWarnings("unchecked")
    default Class<T> extractClass() {
        LambdaMeta meta = Lambdas.extract(this);
        return (Class<T>) meta.getInstantiatedClass();
    }

    /**
     * 字段
     */
    @SuppressWarnings("unchecked")
    default Tuple2<String, Class<?>> getField() {
        LambdaMeta meta = Lambdas.extract(this);
        String fieldName = meta.getFieldName();

        Class<T> clazz = (Class<T>) meta.getInstantiatedClass();
        Field field = ReflectUtil.getField(clazz, fieldName);
        return Tuples.of(fieldName, field.getType());
    }

    /**
     * 字段名
     */
    default String getFieldName() {
        LambdaMeta meta = Lambdas.extract(this);
        return meta.getFieldName();
    }

    @Override
    default <V> GetterFunction<T, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }
}

