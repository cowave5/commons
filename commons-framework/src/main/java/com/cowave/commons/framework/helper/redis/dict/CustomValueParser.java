/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis.dict;

import com.cowave.commons.client.http.asserts.HttpHintException;
import com.cowave.commons.tools.DateUtils;

import java.math.BigDecimal;

import static com.cowave.commons.client.http.constants.HttpCode.BAD_REQUEST;

/**
 * Value值转换器
 *
 * @author shanhuiming
 */
public interface CustomValueParser<T> {

    T parse(Object value);

    static Object getValue(Object value, String valueType, String valueParser){
        if(valueType == null || value == null) {
            return value;
        }
        return switch (valueType) {
            case "int32" -> Integer.valueOf(String.valueOf(value));
            case "int64" -> Long.valueOf(String.valueOf(value));
            case "bool" -> Boolean.valueOf(String.valueOf(value));
            case "float" -> Float.valueOf(String.valueOf(value));
            case "double" -> Double.valueOf(String.valueOf(value));
            case "decimal" -> new BigDecimal(String.valueOf(value));
            case "datetime" -> DateUtils.parse(String.valueOf(value));
            case "custom" -> getCustomValue(value, valueParser);
            default -> value;
        };
    }

    static Object getCustomValue(Object value, String valueParser){
        if(valueParser == null) {
            return null;
        }
        try {
            CustomValueParser parser = (CustomValueParser) Class.forName(valueParser).getDeclaredConstructor().newInstance();
            return parser.parse(value);
        } catch (Exception e) {
            throw new HttpHintException(BAD_REQUEST, "{frame.dict.parse.failed}", value);
        }
    }
}
