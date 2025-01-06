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

/**
 * 简单值转换器
 *
 * @author shanhuiming
 */
public class DefaultValueParser implements DictValueParser {

    @Override
    public Object parse(String value, String param) {
        if(value == null){
            return null;
        }
        return switch (param) {
            case "short" -> Short.parseShort(value);
            case "int" -> Integer.parseInt(value);
            case "long" -> Long.parseLong(value);
            case "float" -> Float.parseFloat(value);
            case "double" -> Double.parseDouble(value);
            case "boolean" -> Boolean.parseBoolean(value);
            default -> value;
        };
    }
}
