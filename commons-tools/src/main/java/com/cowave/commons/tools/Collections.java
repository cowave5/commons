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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jiangbo
 */
@Slf4j
public class Collections {

    /**
     * 集合过滤
     */
    public static <F> List<F> filterToList(Collection<F> collection, Predicate<F> filter) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyList();
        }
        return collection.stream().filter(filter).toList();
    }

    /**
     * 复制集合 -> 新类型的List
     */
    public static <F, T> List<T> copyToList(Collection<F> collection, Class<T> clazz) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyList();
        }
        return collection.stream().map(s -> Converts.copyProperties(s, clazz)).toList();
    }

    /**
     * 复制集合 -> 新类型的List
     */
    public static <F, T> List<T> copyToList(Collection<F> collection, Class<T> clazz, Predicate<F> predicate) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyList();
        }
        return collection.stream().filter(predicate).map(s -> Converts.copyProperties(s, clazz)).toList();
    }

    /**
     * 复制集合 -> Stream
     */
    public static <F, T> Stream<T> copyToStream(Collection<F> collection, Function<F, T> mapper) {
        if (CollectionUtils.isEmpty(collection)) {
            return Stream.empty();
        }
        return collection.stream().map(mapper);
    }

    /**
     * 合并到List
     */
    @SafeVarargs
    public static <T> List<T> mergeToList(Collection<T>... collection) {
        return Arrays.stream(collection).filter(Objects::nonNull).flatMap(Collection::stream).toList();
    }

    /**
     * 合并到List
     */
    @SafeVarargs
    public static <T> List<T> mergeToList(Collection<T> collection, T... t) {
        return Stream.concat(collection.stream(), Stream.of(t)).toList();
    }

    /**
     * 合并到List，并去重
     */
    @SafeVarargs
    public static <T> List<T> mergeToDistinctList(Collection<T>... collection) {
        return Arrays.stream(collection).flatMap(Collection::stream).distinct().toList();
    }

    /**
     * 组装Map，keyList/valueList -> Map
     */
    public static Map<String, String> pairListToMap(List<String> keyList, List<String> valueList) {
        if (keyList.size() != valueList.size()) {
            throw new IllegalArgumentException("无法组装Map, keyList.size=%s, valueList.size=%s".formatted(keyList.size(), valueList.size()));
        }
        Map<String, String> results = new HashMap<>();
        for (int i = 0; i < valueList.size(); i++) {
            results.put(keyList.get(i), valueList.get(i));
        }
        return results;
    }

    /**
     * 组装Map，并转换值类型，keyList/valueList -> Map
     */
    public static <K, T> Map<K, T> pairListToMap(List<K> keyList, List<String> valueList, Class<T> clazz) {
        return pairListToMap(keyList, valueList, value -> Converts.copyProperties(value, clazz));
    }

    /**
     * 组装Map，并转换值类型，keyList/valueList -> Map
     */
    public static <K, T> Map<K, T> pairListToMap(List<K> keyList, List<String> valueList, Function<String, T> function) {
        return pairListToMap(keyList, valueList, function, false);
    }

    /**
     * 组装Map，并转换值类型（忽略空值），keyList/valueList -> Map
     */
    public static <K, T> Map<K, T> pairListToMap(List<K> keyList, List<String> valueList, Function<String, T> function, boolean ignoreNullValue) {
        if (keyList.size() != valueList.size()) {
            throw new IllegalArgumentException("无法组装Map, keyList.size=%s, valueList.size=%s".formatted(keyList.size(), valueList.size()));
        }
        Map<K, T> results = new HashMap<>();
        for (int i = 0; i < valueList.size(); i++) {
            String value = valueList.get(i);
            if (StringUtils.isNotBlank(value)) {
                T t = function.apply(value);
                results.put(keyList.get(i), t);
            } else if (!ignoreNullValue) {
                results.put(keyList.get(i), null);
            }
        }
        return results;
    }

    /**
     * 复制集合 -> set
     */
    public static <F, T> Set<T> copyToSet(Collection<F> collection, Function<F, T> mapper) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptySet();
        }
        return collection.stream().map(mapper).collect(Collectors.toSet());
    }

    /**
     * 复制数组 -> List
     */
    public static <F, T> List<T> arrayToList(F[] array, Function<F, T> mapper) {
        if (array == null || array.length == 0) {
            return java.util.Collections.emptyList();
        }
        return Arrays.stream(array).map(mapper).collect(Collectors.toList());
    }

    /**
     * 复制集合 -> 新类型的List
     */
    public static <F, T> List<T> copyToList(Collection<F> collection, Function<F, T> mapper) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyList();
        }
        return collection.stream().map(mapper).toList();
    }

    /**
     * 复制集合 -> 新类型的List
     */
    public static <F, T> List<T> copyToList(Collection<F> collection, Function<F, T> mapper, Predicate<F> filter) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyList();
        }
        return collection.stream().filter(filter).map(mapper).toList();
    }

    /**
     * 复制集合 -> 新类型的List
     */
    public static <F, T> List<T> copyToList(Collection<F> collection, Class<T> clazz, BiConsumer<F, T> biConsumer) {
        if (collection == null) {
            return java.util.Collections.emptyList();
        }
        return collection.stream().map(c -> {
            T to = null;
            try {
                to = clazz.getDeclaredConstructor().newInstance();
                biConsumer.accept(c, to);
            } catch (Exception e) {
                throw new UnsupportedOperationException(e);
            }
            return to;
        }).toList();
    }

    /**
     * 复制集合 -> 新类型的List
     */
    public static <F, T> List<T> flatCopyToList(Collection<F> collection, Function<F, Stream<T>> mapper) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyList();
        }
        return collection.stream().flatMap(mapper).toList();
    }

    /**
     * 复制集合 -> 新类型的List
     */
    public static <F, T> List<T> flatCopyToList(Collection<F> collection, Function<F, Stream<T>> mapper, Predicate<F> filter) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyList();
        }
        return collection.stream().filter(filter).flatMap(mapper).toList();
    }

    /**
     * 合并到HashMap
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mergeToMap(Map<K, V>... maps) {
        return Stream.of(maps).filter(MapUtils::isNotEmpty)
                .collect(HashMap::new, Map::putAll, Map::putAll);
    }

    /**
     * 合并到LinkedHashMap
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mergeToLinkedMap(Map<K, V>... maps) {
        return Stream.of(maps).filter(MapUtils::isNotEmpty)
                .collect(LinkedHashMap::new, Map::putAll, Map::putAll);
    }

    /**
     * 复制集合 -> Map
     *
     * @param keyMapper Key转换
     */
    public static <K, V> Map<K, V> copyToMap(Collection<V> collection, Function<V, K> keyMapper) {
        return copyToMap(collection, keyMapper, Function.identity());
    }

    /**
     * 复制集合 -> Map
     *
     * @param keyMapper   Key转换
     * @param valueMapper Value转换
     */
    public static <T, K, V> Map<K, V> copyToMap(Collection<T> collection, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyMap();
        }
        return collection.stream().collect(Collectors.toMap(keyMapper, valueMapper, (k1, k2) -> k2));
    }

    /**
     * 复制集合 -> Map
     *
     * @param filter      过滤条件
     * @param keyMapper   Key转换
     * @param valueMapper Value转换
     */
    public static <T, K, V> Map<K, V> copyToMap(Collection<T> collection, Function<T, K> keyMapper, Function<T, V> valueMapper, Predicate<T> filter) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyMap();
        }
        return collection.stream().filter(filter).collect(Collectors.toMap(keyMapper, valueMapper, (k1, k2) -> k2));
    }

    /**
     * 复制集合 -> LinkedMap
     *
     * @param keyMapper Key转换
     */
    public static <K, V> Map<K, V> copyToLinkedMap(Collection<V> collection, Function<V, K> keyMapper) {
        return copyToLinkedMap(collection, keyMapper, Function.identity());
    }

    /**
     * 复制集合 -> LinkedMap
     *
     * @param keyMapper   key转换
     * @param valueMapper value转换
     */
    public static <T, K, V> Map<K, V> copyToLinkedMap(Collection<T> collection, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyMap();
        }
        return collection.stream().collect(Collectors.toMap(keyMapper, valueMapper, (k1, k2) -> k2, LinkedHashMap::new));
    }

    /**
     * 复制集合 -> LinkedMap
     *
     * @param filter      过滤条件
     * @param keyMapper   Key转换
     * @param valueMapper Value转换
     */
    public static <K, T, V> Map<K, V> copyToLinkedMap(Collection<T> collection, Predicate<T> filter, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyMap();
        }
        return collection.stream().filter(filter).collect(Collectors.toMap(keyMapper, valueMapper, (k1, k2) -> k2, LinkedHashMap::new));
    }

    /**
     * 集合分组
     *
     * @param keyMapper 分组Key转换
     */
    public static <K, T> Map<K, List<T>> groupToMap(Collection<T> collection, Function<T, K> keyMapper) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyMap();
        }
        return collection.stream().collect(Collectors.groupingBy(keyMapper));
    }

    /**
     * 集合分组，并过滤
     *
     * @param keyMapper 分组Key转换
     */
    public static <K, T> Map<K, List<T>> groupToMap(Collection<T> collection, Function<T, K> keyMapper, Predicate<T> filter) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyMap();
        }
        return collection.stream().filter(filter).collect(Collectors.groupingBy(keyMapper));
    }

    /**
     * 集合分组，并转换成新类型
     *
     * @param keyMapper 分组Key转换
     * @param clazz     目标类型
     */
    public static <K, T, V> Map<K, List<V>> groupToMap(Collection<T> collection, Function<T, K> keyMapper, Class<V> clazz) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyMap();
        }
        return collection.stream().collect(Collectors.groupingBy(keyMapper, Collectors.mapping(src -> Converts.copyProperties(src, clazz), Collectors.toList())));
    }

    /**
     * 集合分组，并转换成新类型
     *
     * @param keyMapper   分组Key转换
     * @param valueMapper 值转换
     */
    public static <K, T, V> Map<K, List<V>> groupToMap(Collection<T> collection, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyMap();
        }
        return collection.stream().collect(Collectors.groupingBy(keyMapper, Collectors.mapping(valueMapper, Collectors.toList())));
    }

    /**
     * 集合分组，并转换成新类型（进行过滤）
     *
     * @param keyMapper   分组Key转换
     * @param valueMapper 值转换
     * @param filter      过滤函数
     */
    public static <K, T, V> Map<K, List<V>> groupToMap(Collection<T> collection, Function<T, K> keyMapper, Function<T, V> valueMapper, Predicate<T> filter) {
        if (CollectionUtils.isEmpty(collection)) {
            return java.util.Collections.emptyMap();
        }
        return collection.stream().filter(filter).collect(Collectors.groupingBy(keyMapper, Collectors.mapping(valueMapper, Collectors.toList())));
    }

    /**
     * 合并字节数组
     */
    public static byte[] mergeByteArray(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
