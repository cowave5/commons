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

import com.cowave.commons.tools.ReflectUtils;
import com.cowave.commons.tools.lambda.meta.IdeaProxyMeta;
import com.cowave.commons.tools.lambda.meta.ReflectMeta;
import com.cowave.commons.tools.lambda.meta.SerializedMeta;
import com.cowave.commons.tools.lambda.meta.ShadowMeta;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Lambda 解析工具
 *
 * @author jiangbo
 */
public final class Lambdas {

    /**
     * 该缓存可能会在任意不定的时间被清除
     *
     * @param func 需要解析的 lambda 对象
     * @param <T>  类型，被调用的 Function 对象的目标类型
     */
    public static <T> LambdaMeta extract(GetterFunction<T, ?> func) {
        // 1. Idea调试模式下, lambda表达式是一个代理
        if (func instanceof Proxy proxy) {
            return new IdeaProxyMeta(proxy);
        }

        // 2. 反射读取
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            return new ReflectMeta((SerializedMeta) ReflectUtils.setAccessible(method).invoke(func));
        } catch (Exception e) {
            // 3. 反射失败使用序列化的方式读取
            return new ShadowMeta(SerializedMeta.extract(func));
        }
    }
}
