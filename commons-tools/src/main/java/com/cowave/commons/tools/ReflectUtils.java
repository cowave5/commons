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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * @author jiangbo
 */
@Slf4j
public final class ReflectUtils {

    private static final String SETTER_PREFIX = "set";

    private static final String GETTER_PREFIX = "get";

    private static final String CGLIB_CLASS_SEPARATOR = "$$";

    private static ClassLoader systemClassLoader;

    static {
        try {
            systemClassLoader = ClassLoader.getSystemClassLoader();
        } catch (SecurityException ignored) {}
    }

    /**
     * 获取对象类型，代理则获取其父类型
     */
    public static Class<?> getClassIgnoreProxy(Object instance){
        if (instance == null){
            throw new RuntimeException("Instance must not be null");
        }

        Class<?> clazz = instance.getClass();
        if (clazz != null && clazz.getName().contains(CGLIB_CLASS_SEPARATOR)){
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && !Object.class.equals(superClass)){
                return superClass;
            }
        }
        return clazz;
    }

    /**
     * 获取范型接口的参数类型
     *
     * @param clazz             当前类
     * @param interfaceClazz    拥有相同范型的接口
     */
    public static <T> Type getGenericInterfaceType(Class<? extends T> clazz, Class<T> interfaceClazz) {
        return Arrays.stream(clazz.getGenericInterfaces())
                .filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast)
                .filter(type -> Objects.equals(type.getRawType(), interfaceClazz))
                .findFirst()
                .map(type -> {
                    Type[] types = type.getActualTypeArguments();
                    if(types.length != 1){
                        throw new UnsupportedOperationException("Generics not found");
                    }
                    return types[0];
                }).orElseThrow(() -> new UnsupportedOperationException("Generics not found"));

    }

    /**
     * 获取Class的参数类型
     */
    public static Class<?> getClassGenericType(final Class<?> clazz){
        return getClassGenericType(clazz, 0);
    }

    /**
     * 获取Class的泛型参数类型，找不到则返回Object.class.
     */
    public static Class<?> getClassGenericType(final Class<?> clazz, final int index){
        Type genType = clazz.getGenericSuperclass();
        if (!(genType instanceof ParameterizedType)){
            return Object.class;
        }

        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        if (index >= params.length || index < 0){
            return Object.class;
        }

        if (!(params[index] instanceof Class)) {
            return Object.class;
        }
        return (Class<?>) params[index];
    }

    /**
     * Getter调用
     * @param obj 对象
     * @param propertyName 支持多级，如：对象名.对象名.方法
     */
    @SuppressWarnings("unchecked")
    public static <E> E invokeGetter(Object obj, String propertyName){
        Object object = obj;
        for (String name : StringUtils.split(propertyName, ".")){
            String getterMethodName = GETTER_PREFIX + StringUtils.capitalize(name);
            object = invokeMethod(object, getterMethodName, new Class[] {}, new Object[] {});
        }
        return (E) object;
    }

    /**
     * Setter调用
     * @param obj 对象
     * @param propertyName 支持多级，如：对象名.对象名.方法
     */
    public static <E> void invokeSetter(Object obj, String propertyName, E value){
        Object object = obj;
        String[] names = StringUtils.split(propertyName, ".");
        for (int i = 0; i < names.length; i++){
            if (i < names.length - 1){
                String getterMethodName = GETTER_PREFIX + StringUtils.capitalize(names[i]);
                object = invokeMethod(object, getterMethodName, new Class[] {}, new Object[] {});
            }else{
                String setterMethodName = SETTER_PREFIX + StringUtils.capitalize(names[i]);
                invokeMethodByName(object, setterMethodName, new Object[] { value });
            }
        }
    }

    /**
     * 获取对象属性，没有的属性返回NULL
     * @param obj       对象
     * @param fieldName 属性名称
     */
    @SuppressWarnings("unchecked")
    public static <E> E getFieldValue(final Object obj, final String fieldName){
        Field field = getAccessibleField(obj, fieldName);
        if (field == null){
            return null;
        }

        E result = null;
        try{
            result = (E) field.get(obj);
        }catch (IllegalAccessException e){
            // never
        }
        return result;
    }

    /**
     * 设置对象属性，没有的属性则忽略
     * @param obj       对象
     * @param fieldName 属性名称
     * @param value     属性值
     */
    public static <E> void setFieldValue(final Object obj, final String fieldName, final E value){
        Field field = getAccessibleField(obj, fieldName);
        if (field == null){
            return;
        }

        try{
            field.set(obj, value);
        }catch (IllegalAccessException e){
            // never
        }
    }

    /**
     * 调用对象方法，没有的方法则忽略
     *
     * @param obj            对象
     * @param methodName     方法名称
     * @param parameterTypes 参数类型
     * @param args           参数值
     */
    @SuppressWarnings("unchecked")
    public static <E> E invokeMethod(final Object obj, final String methodName, final Class<?>[] parameterTypes, final Object[] args){
        if (obj == null || methodName == null){
            return null;
        }

        Method method = getAccessibleMethod(obj, methodName, parameterTypes);
        if (method == null){
            return null;
        }

        try{
            return (E) method.invoke(obj, args);
        }catch (Exception e){
            String msg = "method: " + method + ", obj: " + obj + ", args: " + Arrays.toString(args) + "";
            throw convertReflectionExceptionToUnchecked(msg, e);
        }
    }

    /**
     * 调用对象方法，只匹配函数名，如果有多个同名函数调用第一个，没有匹配则忽略
     *
     * @param obj        对象
     * @param methodName 方法命名
     * @param args       参数列表
     */
    public static void invokeMethodByName(final Object obj, final String methodName, final Object[] args){
        Method method = getAccessibleMethodByName(obj, methodName, args.length);
        if (method == null){
            return;
        }

        try{
            Class<?>[] cs = method.getParameterTypes(); // 类型转换（将参数数据类型转换为目标方法参数类型）
            for (int i = 0; i < cs.length; i++){
                if (args[i] != null && !args[i].getClass().equals(cs[i])){
                    if (cs[i] == String.class){
                        args[i] = Converts.toStr(args[i]);
                        if (StringUtils.endsWith((String) args[i], ".0")){
                            args[i] = StringUtils.substringBefore((String) args[i], ".0");
                        }
                    }else if (cs[i] == Integer.class){
                        args[i] = Converts.toInt(args[i]);
                    }else if (cs[i] == Long.class){
                        args[i] = Converts.toLong(args[i]);
                    }else if (cs[i] == Double.class){
                        args[i] = Converts.toDouble(args[i]);
                    }else if (cs[i] == Float.class){
                        args[i] = Converts.toFloat(args[i]);
                    }else if (cs[i] == Date.class){
                        if (args[i] instanceof String){
                            args[i] = DateUtils.parse((String)args[i]);
                        }else{
                            args[i] = DateUtils.getDate((Double)args[i]);
                        }
                    }else if (cs[i] == boolean.class || cs[i] == Boolean.class){
                        args[i] = Converts.toBool(args[i]);
                    }
                }
            }
            method.invoke(obj, args);
        }catch (Exception e){
            String msg = "method: " + method + ", obj: " + obj + ", args: " + Arrays.toString(args) + "";
            throw convertReflectionExceptionToUnchecked(msg, e);
        }
    }

    /**
     * 获取对象属性Field，递归父类寻找
     * @param obj        对象
     * @param fieldName  属性名称
     */
    public static Field getAccessibleField(final Object obj, final String fieldName){
        if (obj == null){
            return null;
        }

        Validate.notBlank(fieldName, "fieldName can't be blank");
        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()){
            try{
                Field field = superClass.getDeclaredField(fieldName);
                makeAccessible(field);
                return field;
            }catch (NoSuchFieldException ignored){}
        }
        return null;
    }

    /**
     * 获取对象方法Field，递归父类寻找
     * @param obj            对象
     * @param methodName     方法名称
     * @param parameterTypes 参数类型列表
     */
    public static Method getAccessibleMethod(final Object obj, final String methodName, final Class<?>... parameterTypes){
        if (obj == null){
            return null;
        }

        Validate.notBlank(methodName, "methodName can't be blank");
        for (Class<?> searchType = obj.getClass(); searchType != Object.class; searchType = searchType.getSuperclass()){
            try{
                Method method = searchType.getDeclaredMethod(methodName, parameterTypes);
                makeAccessible(method);
                return method;
            }catch (NoSuchMethodException ignored){}
        }
        return null;
    }

    /**
     * 获取对象方法Field，递归父类寻找
     * @param obj        对象
     * @param methodName 方法名称
     * @param argsNum    参数数量
     */
    public static Method getAccessibleMethodByName(final Object obj, final String methodName, int argsNum){
        if (obj == null){
            return null;
        }

        Validate.notBlank(methodName, "methodName can't be blank");
        for (Class<?> searchType = obj.getClass(); searchType != Object.class; searchType = searchType.getSuperclass()){
            Method[] methods = searchType.getDeclaredMethods();
            for (Method method : methods){
                if (method.getName().equals(methodName) && method.getParameterTypes().length == argsNum){
                    makeAccessible(method);
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * 修改方法访问权限
     */
    public static void makeAccessible(Method method){
        if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
                && !method.canAccess(null)){
            method.setAccessible(true);
        }
    }

    /**
     * 修改属性访问权限
     */
    public static void makeAccessible(Field field){
        if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())
                || Modifier.isFinal(field.getModifiers())) && !field.canAccess(null)){
            field.setAccessible(true);
        }
    }

    public static <T extends AccessibleObject> T setAccessible(T object) {
        return AccessController.doPrivileged(new SetAccessibleAction<>(object));
    }

    public static Class<?> loadClass(String name, ClassLoader classLoader) {
        try {
            return loadClass(name, getClassLoaders(classLoader));
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(e.getMessage());
        }
    }

    private static Class<?> loadClass(String className, ClassLoader[] classLoaders) throws ClassNotFoundException {
        for (ClassLoader classLoader : classLoaders) {
            if (classLoader != null) {
                try {
                    return Class.forName(className, true, classLoader);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
        }
        throw new ClassNotFoundException("Cannot find class: " + className);
    }

    private static ClassLoader[] getClassLoaders(ClassLoader classLoader) {
        return new ClassLoader[]{classLoader, Thread.currentThread().getContextClassLoader(),
                ReflectUtils.class.getClassLoader(), systemClassLoader};
    }

    public static class SetAccessibleAction<T extends AccessibleObject> implements PrivilegedAction<T> {
        private final T obj;

        public SetAccessibleAction(T obj) {
            this.obj = obj;
        }

        @Override
        public T run() {
            obj.setAccessible(true);
            return obj;
        }
    }

    private static RuntimeException convertReflectionExceptionToUnchecked(String msg, Exception e){
        if (e instanceof IllegalAccessException || e instanceof IllegalArgumentException || e instanceof NoSuchMethodException){
            return new IllegalArgumentException(msg, e);
        }else if (e instanceof InvocationTargetException){
            return new RuntimeException(msg, ((InvocationTargetException) e).getTargetException());
        }
        return new RuntimeException(msg, e);
    }
}
