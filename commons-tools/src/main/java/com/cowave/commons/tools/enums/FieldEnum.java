/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.enums;

import cn.hutool.core.util.ClassUtil;
import com.alibaba.fastjson.JSON;
import com.cowave.commons.tools.Converts;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * @author jiangbo
 */
@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FieldEnum {

    /**
     * 字符串
     */
    STRING(0),

    /**
     * 数字
     */
    NUMBER(1),

    /**
     * 整数
     */
    NUMBER_LONG(102),

    /**
     * 浮点数
     */
    NUMBER_DOUBLE(103),

    /**
     * 枚举
     */
    ENUM(2),

    /**
     * 布尔值
     */
    BOOL(3),

    /**
     * ip
     */
    IPV4(4),

    /**
     * 数组（包括集合）
     */
    ARRAY(5),

    /**
     * 键值对
     */
    MAP(6),

    /**
     * 时间戳
     */
    TIMESTAMP(7),

    /**
     * 日期
     */
    DATE(8),

    /**
     * 未知
     */
    NONE(-1),
    ;

    @JsonValue
    private final int key;

    private static final Map<Integer, FieldEnum> FIELD_TYPE_MAP =
            Arrays.stream(FieldEnum.values()).collect(Collectors.toMap(FieldEnum::getKey, Function.identity()));

    /**
     * 获取Field类型，根据对象实例
     */
    public static FieldEnum of(Object obj) {
        if (obj == null) {
            return FieldEnum.NONE;
        }
        return of(obj.getClass());
    }

    /**
     * 获取Field类型，根据Class类型
     */
    public static FieldEnum of(Class<?> type) {
        if (type == null) {
            return FieldEnum.NONE;
        }

        if (String.class.isAssignableFrom(type)) {
            return FieldEnum.STRING;
        } else if (Map.class.isAssignableFrom(type)) {
            return FieldEnum.STRING;
        } else if (Collection.class.isAssignableFrom(type)) {
            return FieldEnum.STRING;
        } else if (Boolean.class.isAssignableFrom(type)) {
            return FieldEnum.BOOL;
        } else if (Long.class.isAssignableFrom(type) || Integer.class.isAssignableFrom(type) || Short.class.isAssignableFrom(type)) {
            return FieldEnum.NUMBER_LONG;
        } else if (Double.class.isAssignableFrom(type) || Float.class.isAssignableFrom(type)) {
            return FieldEnum.NUMBER_LONG;
        } else if (Number.class.isAssignableFrom(type)) {
            return FieldEnum.NUMBER;
        } else if (ClassUtil.isBasicType(type)) {
            return FieldEnum.NUMBER;
        } else if (ClassUtil.isEnum(type)) {
            return FieldEnum.ENUM;
        } else if (Date.class.isAssignableFrom(type)) {
            return FieldEnum.DATE;
        }
        return FieldEnum.NONE;
    }

    /**
     * 获取Field类型，根据枚举字面量
     */
    public static FieldEnum of(String type) {
        if (StringUtils.isBlank(type)) {
            return FieldEnum.NONE;
        }
        try {
            return FieldEnum.valueOf(type.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("unknown Type: %s".formatted(type));
        }
    }


    /**
     * 获取Field类型，根据枚举值
     */
    public static FieldEnum of(Integer type) {
        return MapUtils.getObject(FIELD_TYPE_MAP, type, NONE);
    }

    /**
     * 尝试转换类型
     */
    public static Object tryConvert(FieldEnum fieldEnum, Object obj) {
        if (fieldEnum == null) {
            return obj;
        }
        return switch (fieldEnum) {
            case STRING -> tryConvert(Converts::toJson, obj);
            case NUMBER -> tryConvert(s -> {
                Class<?> clazz = s.getClass();
                return (Number.class.isAssignableFrom(clazz) || ClassUtil.isEnum(clazz)) ? s : Converts.toBigDecimal(s);
            }, obj);
            case NUMBER_LONG, TIMESTAMP -> tryConvert(Converts::toLong, obj);
            case NUMBER_DOUBLE -> tryConvert(Converts::toDouble, obj);
            case BOOL -> tryConvert(Converts::toBool, obj);
            case IPV4, DATE -> tryConvert(Converts::toStr, obj);
            case ARRAY, MAP -> tryConvert(s -> JSON.parse(Converts.toJson(s)), obj);
            default -> obj;
        };
    }

    private static Object tryConvert(UnaryOperator<Object> mapping, Object src) {
        Object dst = mapping.apply(src);
        return dst == null ? src : dst;
    }

    /**
     * 是否数字
     */
    public static boolean isNumber(FieldEnum fieldEnum) {
        return NUMBER.equals(fieldEnum) || NUMBER_LONG.equals(fieldEnum) || NUMBER_DOUBLE.equals(fieldEnum);
    }

}
