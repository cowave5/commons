/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.annotation;

import com.cowave.commons.tools.Converts;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author jiangbo
 */
@Slf4j
public class Annotations {

    /**
     * 获取package下所有注解
     */
    public static <T extends Annotation> List<AnnotationMeta<T>> getAnnotations(List<String> packages, Class<T> clazz) {
        return getAnnotations(packages, null, clazz);
    }

    /**
     * 获取package下所有注解
     */
    public static <T extends Annotation> List<AnnotationMeta<T>> getAnnotations(List<String> packages, List<String> excludes, Class<T> clazz) {
        packages = distinctPackage(packages);
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages(packages.toArray(new String[0])));
        return getAnnotations(reflections, excludes, clazz).toList();
    }

    /**
     * 获取package下所有子类
     */
    public static <T> List<Class<? extends T>> getSubClasses(List<String> packages, List<String> excludes, Class<T> clazz) {
        packages = distinctPackage(packages);
        Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages(packages.toArray(new String[0])));
        return getSubClasses(reflections, excludes, clazz).toList();
    }

    private static <T extends Annotation> Stream<AnnotationMeta<T>> getAnnotations(Reflections reflections, List<String> excludes, Class<T> clazz) {
        return reflections.getTypesAnnotatedWith(clazz).stream().map(c -> {
                    if (excludes != null && excludes.stream().anyMatch(ex -> c.getName().startsWith(ex))) {
                        log.debug("getAnnotations exclude class: {}", clazz);
                        return null;
                    }
                    T t = c.getAnnotation(clazz);
                    return t == null ? null : new AnnotationMeta<>(c, t);
                }).filter(Objects::nonNull);
    }

    private static <T> Stream<Class<? extends T>> getSubClasses(Reflections reflections, List<String> excludes, Class<T> clazz) {
        Stream<? extends Class<? extends T>> stream =
                reflections.getSubTypesOf(clazz).stream().map(c -> {
                    if (excludes != null && excludes.stream().anyMatch(ex -> c.getName().startsWith(ex))) {
                        log.debug("getSubClasses exclude class: {}", c);
                        return null;
                    }
                    return c;
                }).filter(Objects::nonNull);
        return Converts.cast(stream);
    }

    private static List<String> distinctPackage(List<String> packages) {
        List<String> list = new ArrayList<>();
        packages.stream().sorted(Comparator.comparingInt(String::length))
                .forEach(p -> {
                    if (list.stream().noneMatch(p::startsWith)) {
                        list.add(p);
                    }
                });
        return list;
    }
}
