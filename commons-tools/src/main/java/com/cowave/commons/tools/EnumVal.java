/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * @author shanhuiming
 */
public interface EnumVal<T> {

    static <E extends Enum<E>> E getInstance(Class<E> clazz, String name) {
        for (E e : clazz.getEnumConstants()) {
            if (e.name().equals(name)) {
                return e;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static <T, E extends Enum<E> & EnumVal<T>> List<T> vals(Class<E> clazz) {
        try {
            Method method = clazz.getMethod("values");
            EnumVal<T>[] enums = (EnumVal<T>[]) method.invoke(null);
            return Collections.arrayToList(enums, EnumVal::getVal);
        } catch (Exception e) {
            throw new UnsupportedOperationException(clazz + " isn't a enum");
        }
    }

    default boolean equalsName(String name) {
        if (this instanceof Enum<?>) {
            return ((Enum<?>) this).name().equals(name);
        }
        return false;
    }

    default boolean equalsVal(T val) {
        return Objects.equals(this.getVal(), val);
    }

    default T getVal(){
        return null;
    }

    default String getDesc(){
        return "";
    }
}
