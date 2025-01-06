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

import cn.hutool.core.util.EnumUtil;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.cowave.commons.tools.json.JacksonUtils;

import java.lang.reflect.Type;

/**
 * fastjson使用jackson序列化
 *
 * @author jiangbo
 */
public class JacksonSerializer implements ObjectSerializer {

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) {
        if (object == null) {
            serializer.writeNull();
            return;
        }

        if (EnumUtil.isEnum(object)) {
            serializer.writeWithFieldName(JacksonUtils.writeValue(object), fieldName);
        } else {
            Object json = JacksonUtils.convert(object, Object.class);
            serializer.writeWithFieldName(json, fieldName);
        }
    }
}
