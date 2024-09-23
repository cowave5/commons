/*
 * Copyright (c) 2017～2099 Cowave All Rights Reserved.
 *
 * For licensing information, please contact: https://www.cowave.com.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
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