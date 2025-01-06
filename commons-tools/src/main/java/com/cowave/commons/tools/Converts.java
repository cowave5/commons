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

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.cowave.commons.tools.json.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.*;

/**
 * @author jiangbo
 */
@Slf4j
public class Converts {

    /**
     * 获取参数，指定默认值
     */
    public static <T> T nvl(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * 类型强转
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj) {
        return (T) obj;
    }

    /**
     * 转字符串
     */
    @Nullable
    public static String toStr(Object value) {
        return toStr(value, null);
    }

    /**
     * 转字符串
     */
    public static String toStr(Object value, String defaultValue) {
        return Objects.toString(value, defaultValue);
    }

    /**
     * 转字符串, UTF-8
     */
    public static String toEncodeStr(Object obj) {
        return toEncodeStr(obj, "UTF-8");
    }

    /**
     * 转字符串
     */
    public static String toEncodeStr(Object obj, String charsetName) {
        return toEncodeStr(obj, Charset.forName(charsetName));
    }

    /**
     * 转字符串
     */
    public static String toEncodeStr(Object obj, Charset charset) {
        if (null == obj) {
            return null;
        }

        if (obj instanceof String str) {
            return str;
        } else if (obj instanceof byte[] bytes) {
            return toEncodeStr(bytes, charset);
        } else if (obj instanceof Byte[] bs) {
            byte[] bytes = ArrayUtils.toPrimitive(bs);
            return toEncodeStr(bytes, charset);
        } else if (obj instanceof ByteBuffer bb) {
            return toEncodeStr(bb, charset);
        }
        return obj.toString();
    }

    /**
     * 转字符串
     */
    public static String toEncodeStr(byte[] bytes, String charset) {
        return toEncodeStr(bytes, StringUtils.isEmpty(charset) ? Charset.defaultCharset() : Charset.forName(charset));
    }

    /**
     * 转字符串
     */
    public static String toEncodeStr(byte[] data, Charset charset) {
        if (data == null) {
            return null;
        }

        if (null == charset) {
            return new String(data);
        }
        return new String(data, charset);
    }

    /**
     * 转字符串
     */
    public static String toEncodeStr(ByteBuffer buffer, String charset) {
        if (buffer == null) {
            return null;
        }
        return toEncodeStr(buffer, Charset.forName(charset));
    }

    /**
     * 转字符串
     */
    public static String toEncodeStr(ByteBuffer buffer, Charset charset) {
        if (null == charset) {
            charset = Charset.defaultCharset();
        }
        return charset.decode(buffer).toString();
    }

    /**
     * 转Json字符串
     */
    @Nullable
    public static String toJson(Object value) {
        return toJson(value, null);
    }

    /**
     * 转Json字符串
     */
    public static String toJson(Object value, String defaultValue) {
        if (null == value) {
            return defaultValue;
        }

        try {
            if (value instanceof String str) {
                return str;
            }
            if (value.getClass().isEnum()) {
                return StringUtils.strip(JacksonUtils.writeValue(value), "\"");
            }
            return JacksonUtils.writeValue(value);
        } catch (Exception e) {
            log.warn("Json converts failed, use default value");
            return defaultValue;
        }
    }

    /**
     * 转Char字符
     */
    @Nullable
    public static Character toChar(Object value) {
        return toChar(value, null);
    }

    /**
     * 转Char字符，取字符串首字符
     */
    public static Character toChar(Object value, Character defaultValue) {
        if (null == value) {
            return defaultValue;
        }
        if (value instanceof Character c) {
            return c;
        }
        final String valueStr = toStr(value, null);
        return StringUtils.isEmpty(valueStr) ? defaultValue : valueStr.charAt(0);
    }

    /**
     * 转Byte
     */
    @Nullable
    public static Byte toByte(Object value) {
        return toByte(value, null);
    }

    /**
     * 转换为byte
     */
    public static Byte toByte(Object value, Byte defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Byte b) {
            return b;
        }
        if (value instanceof Number nb) {
            return nb.byteValue();
        }

        String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return Byte.parseByte(valueStr);
        } catch (Exception e) {
            log.warn("Converts.toByte failed with {}, return default value {}", valueStr, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转Short
     */
    @Nullable
    public static Short toShort(Object value) {
        return toShort(value, null);
    }

    /**
     * 转Short
     */
    public static Short toShort(Object value, Short defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Short s) {
            return s;
        }
        if (value instanceof Number nb) {
            return nb.shortValue();
        }

        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return Short.parseShort(valueStr.trim());
        } catch (Exception e) {
            log.warn("Converts.toShort failed with {}, return default value {}", valueStr, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转Number
     */
    @Nullable
    public static Number toNumber(Object value) {
        return toNumber(value, null);
    }

    /**
     * 转Number
     */
    public static Number toNumber(Object value, Number defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number nb) {
            return nb;
        }

        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return NumberFormat.getInstance().parse(valueStr);
        } catch (Exception e) {
            log.warn("Converts.toNumber failed with {}, return default value {}", valueStr, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转int
     */
    @Nullable
    public static Integer toInt(Object value) {
        return toInt(value, null);
    }

    /**
     * 转int
     */
    public static Integer toInt(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number nb) {
            return nb.intValue();
        }

        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(valueStr.trim());
        } catch (Exception e) {
            log.warn("Converts.toInt failed with {}, return default value {}", valueStr, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转long
     */
    @Nullable
    public static Long toLong(Object value) {
        return toLong(value, null);
    }

    /**
     * 转long
     */
    public static Long toLong(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number nb) {
            return nb.longValue();
        }

        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return new BigDecimal(valueStr.trim()).longValue();
        } catch (Exception e) {
            log.warn("Converts.toLong failed with {}, return default value {}", valueStr, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转Float
     */
    @Nullable
    public static Float toFloat(Object value) {
        return toFloat(value, null);
    }

    /**
     * 转Float
     */
    public static Float toFloat(Object value, Float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Float f) {
            return f;
        }
        if (value instanceof Number nb) {
            return nb.floatValue();
        }

        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return Float.parseFloat(valueStr.trim());
        } catch (Exception e) {
            log.warn("Converts.toFloat failed with {}, return default value {}", valueStr, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转double
     */
    @Nullable
    public static Double toDouble(Object value) {
        return toDouble(value, null);
    }

    /**
     * 转double
     */
    public static Double toDouble(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Double d) {
            return d;
        }
        if (value instanceof Number nb) {
            return nb.doubleValue();
        }

        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return new BigDecimal(valueStr.trim()).doubleValue();
        } catch (Exception e) {
            log.warn("Converts.toDouble failed with {}, return default value {}", valueStr, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转Boolean，支持：true/false, yes/no/ok, 1/0
     */
    @Nullable
    public static Boolean toBool(Object value) {
        return toBool(value, null);
    }

    /**
     * 转Boolean，支持：true/false, yes/no/ok, 1/0
     */
    public static Boolean toBool(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bl) {
            return bl;
        }

        String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        valueStr = valueStr.trim().toLowerCase();
        return switch (valueStr) {
            case "true", "yes", "ok", "1" -> true;
            case "false", "no", "0" -> false;
            default -> defaultValue;
        };
    }

    /**
     * 转BigInteger
     */
    @Nullable
    public static BigInteger toBigInteger(Object value) {
        return toBigInteger(value, null);
    }

    /**
     * 转BigInteger
     */
    public static BigInteger toBigInteger(Object value, BigInteger defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof BigInteger bi) {
            return bi;
        }
        if (value instanceof Long l) {
            return BigInteger.valueOf(l);
        }

        String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return new BigInteger(valueStr);
        } catch (Exception e) {
            log.warn("Converts.toBigInteger failed with {}, return default value {}", valueStr, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转BigDecimal
     */
    @Nullable
    public static BigDecimal toBigDecimal(Object value) {
        return toBigDecimal(value, null);
    }

    /**
     * 转BigDecimal
     */
    public static BigDecimal toBigDecimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number nb) {
            return new BigDecimal(String.valueOf(nb));
        }
        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return new BigDecimal(valueStr);
        } catch (Exception e) {
            log.warn("Converts.toBigDecimal failed with {}, return default value {}", valueStr, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转Enum
     */
    @Nullable
    public static <E extends Enum<E>> E toEnum(Class<E> clazz, Object value) {
        return toEnum(clazz, value, null);
    }

    /**
     * 转Enum
     */
    public static <E extends Enum<E>> E toEnum(Class<E> clazz, Object value, E defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (clazz.isAssignableFrom(value.getClass())) {
            @SuppressWarnings("unchecked")
            E e = (E) value;
            return e;
        }

        final String valueStr = toStr(value, null);
        if (StringUtils.isEmpty(valueStr)) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(clazz, valueStr);
        } catch (Exception e) {
            log.warn("Converts.toEnum failed with {}, return default value {}", valueStr, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 转String数组，分隔符","
     */
    public static String[] toStrArray(String str) {
        return toStrArray(",", str);
    }

    /**
     * 转String数组
     */
    public static String[] toStrArray(String split, String str) {
        return str.split(split);
    }

    /**
     * 转Int数组，分隔符","
     */
    public static Integer[] toIntArray(String str) {
        return toIntArray(str, ",");
    }

    /**
     * 转Int数组
     */
    public static Integer[] toIntArray(String str, String split) {
        if (StringUtils.isEmpty(str)) {
            return new Integer[]{};
        }
        return Arrays.stream(str.split(split)).map(s -> toInt(s, 0)).toArray(Integer[]::new);
    }

    /**
     * 转Long数组，分隔符","
     */
    public static Long[] toLongArray(String str) {
        return toLongArray(",", str);
    }

    /**
     * 转Long数组
     */
    public static Long[] toLongArray(String split, String str) {
        if (StringUtils.isEmpty(str)) {
            return new Long[]{};
        }
        return Arrays.stream(str.split(split)).map(s -> toLong(s, 0L)).toArray(Long[]::new);
    }

    /**
     * 半角 -> 全角
     */
    public static String toSbc(String input) {
        return toSbc(input, null);
    }

    /**
     * 半角 -> 全角
     */
    public static String toSbc(String input, Set<Character> notConvertSet) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (null != notConvertSet && notConvertSet.contains(c[i])) {
                continue; // 跳过不替换的字符
            }

            if (c[i] == ' ') {
                c[i] = '\u3000';
            } else if (c[i] < '\177') {
                c[i] = (char) (c[i] + 65248);
            }
        }
        return new String(c);
    }

    /**
     * 全角 -> 半角
     */
    public static String toDbc(String input) {
        return toDbc(input, null);
    }

    /**
     * 全角 -> 半角
     */
    public static String toDbc(String text, Set<Character> notConvertSet) {
        char[] c = text.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (null != notConvertSet && notConvertSet.contains(c[i])) {
                continue; // 跳过不替换的字符
            }

            if (c[i] == '\u3000') {
                c[i] = ' ';
            } else if (c[i] > '\uFF00' && c[i] < '\uFF5F') {
                c[i] = (char) (c[i] - 65248);
            }
        }
        return new String(c);
    }

    /**
     * 数字金额大写
     */
    public static String digitUppercase(double n) {
        String[] fraction = {"角", "分"};
        String[] digit = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        String[][] unit = {{"元", "万", "亿"}, {"", "拾", "佰", "仟"}};

        String head = n < 0 ? "负" : "";
        n = Math.abs(n);

        StringBuilder s = new StringBuilder();
        for (int i = 0; i < fraction.length; i++) {
            s.append((digit[(int) (Math.floor(n * 10 * Math.pow(10, i)) % 10)] + fraction[i]).replaceAll("(零.)+", ""));
        }
        if (s.length() < 1) {
            s = new StringBuilder("整");
        }

        int integerPart = (int) Math.floor(n);
        for (int i = 0; i < unit[0].length && integerPart > 0; i++) {
            StringBuilder p = new StringBuilder();
            for (int j = 0; j < unit[1].length && n > 0; j++) {
                p.insert(0, digit[integerPart % 10] + unit[1][j]);
                integerPart = integerPart / 10;
            }
            s.insert(0, p.toString().replaceAll("(零.)*零$", "").replaceAll("^$", "零") + unit[0][i]);
        }
        return head + s.toString().replaceAll("(零.)*零元", "元").replaceFirst("(零.)+", "").replaceAll("(零.)+", "零").replaceAll("^整$", "零元整");
    }

    /**
     * 复制属性
     */
    public static <S, T> T copyProperties(S src, Class<T> dest) {
        return BeanUtil.copyProperties(src, dest);
    }

    /**
     * 复制属性
     */
    public static <S, T> void copyProperties(S src, T dest) {
        BeanUtil.copyProperties(src, dest);
    }

    /**
     * 从List(Map) 获取期望类型
     */
    public static <T> T firstOfMapList(List<Map<String, Object>> list, Class<T> clazz) {
        Object obj;
        if (List.class.isAssignableFrom(clazz)) {
            obj = list;
        } else if (Map.class.isAssignableFrom(clazz)) {
            obj = CollectionUtils.isEmpty(list) ? java.util.Collections.emptyMap() : list.get(0);
        } else {
            obj = mapToClazz(list, clazz);
        }
        return Converts.cast(obj);
    }

    private static <T> Object mapToClazz(List<Map<String, Object>> list, Class<T> clazz) {
        Map<String, Object> map = CollectionUtils.isEmpty(list) ? java.util.Collections.emptyMap() : list.get(0);
        Object value = null;
        if (MapUtils.isNotEmpty(map)) {
            List<Object> values = new ArrayList<>(map.values());
            value = values.get(0);
            if (value instanceof List<?> valueList) {
                value = CollectionUtils.isEmpty(valueList) ? null : valueList.get(0);
            }
        }
        return toClazz(value, clazz);
    }

    private static <T> T toClazz(Object value, Class<T> clazz) {
        Object obj = value;
        if (Number.class.isAssignableFrom(clazz)) {
            value = value == null ? 0 : value;
            try {
                obj = JSON.parseObject(JSON.toJSONString(value), clazz);
            } catch (Exception e) {
                log.warn("Converts.firstOfMapList failed with {}", value);
                obj = 0;
            }
        } else if (value != null) {
            obj = BeanUtil.copyProperties(value, clazz);
        }
        return Converts.cast(obj);
    }
}
