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

    T val();

    String desc();

    default boolean isEqual(T val) {
        return Objects.equals(this.val(), val);
    }

    @SuppressWarnings("unchecked")
    default List<T> enumVals() {
        try {
            Method method = this.getClass().getMethod("values");
            EnumVal<T>[] enumVals = (EnumVal<T>[]) method.invoke(null);
            return Collections.arrayToList(enumVals, EnumVal::val);
        } catch (Exception e) {
            throw new UnsupportedOperationException(this.getClass() + " isn't a enum");
        }
    }
}
