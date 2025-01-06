/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.json;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.cowave.commons.tools.Converts;
import com.cowave.commons.tools.json.JacksonUtils;

import java.lang.reflect.Type;

/**
 * fastjson使用jackson反序列化
 *
 * @author jiangbo
 */
public class JacksonDeserializer implements ObjectDeserializer {
    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        Object value = parser.parse();
        if (type instanceof Class<?> clazz && clazz.isEnum()) {
            String obj = Converts.toStr(value);
            try {
                if (obj == null) {
                    return null;
                }
                return JacksonUtils.readValue(obj, type);
            } catch (Exception e) {
                try {
                    return (T) Enum.valueOf(Converts.cast(clazz), obj);
                } catch (Exception ex) {
                    return null;
                }
            }
        }

        try {
            return JacksonUtils.convert(value, type);
        } catch (Exception e) {
            return (T) value;
        }
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
