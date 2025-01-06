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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 *
 * @author shanhuiming
 *
 */
@Component
public final class SpringContext implements BeanFactoryPostProcessor, ApplicationContextAware{

    private static ConfigurableListableBeanFactory beanFactory;

    private static ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException{
        SpringContext.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException{
        SpringContext.applicationContext = applicationContext;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> List<T> getBeanList(Class<T> clz) throws BeansException{
        String[] beanNames = beanFactory.getBeanNamesForType(clz);
        List list = new ArrayList<>();
        if(ObjectUtils.isEmpty(beanNames)){
            return list;
        }

        for(String beanName : beanNames){
            list.add(getBean(beanName));
        }
        return list;
    }

    /**
     * 获取bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException{
        try{
            return (T) beanFactory.getBean(name);
        }catch(NoSuchBeanDefinitionException e){
            return null;
        }
    }

    /**
     * 获取bean
     */
    public static <T> T getBean(Class<T> clz) throws BeansException{
        return beanFactory.getBean(clz);
    }

    /**
     * 是否包含bean
     */
    public static boolean containsBean(String name){
        return beanFactory.containsBean(name);
    }

    /**
     * 是否单例
     */
    public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException{
        return beanFactory.isSingleton(name);
    }

    /**
     * 注册对象的类型
     */
    public static Class<?> getType(String name) throws NoSuchBeanDefinitionException{
        return beanFactory.getType(name);
    }

    /**
     * 注册对象的类型别名
     */
    public static String[] getAliases(String name) throws NoSuchBeanDefinitionException{
        return beanFactory.getAliases(name);
    }

    /**
     * 获取代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAopProxy(){
        return (T) AopContext.currentProxy();
    }

    /**
     * 获取当前的环境配置，无配置返回null
     */
    public static String[] getActiveProfiles(){
        return applicationContext.getEnvironment().getActiveProfiles();
    }

    /**
     * 获取当前的环境配置，当有多个环境配置时取第一个
     */
    public static String getActiveProfile(){
        final String[] activeProfiles = getActiveProfiles();
        return ObjectUtils.isNotEmpty(activeProfiles) ? activeProfiles[0] : null;
    }

    /**
     * 获取环境变量
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProperty(String key, T defaultValue) {
        T value = (T)applicationContext.getEnvironment().getProperty(key);
        if(value != null) {
            return value;
        }
        return defaultValue;
    }
}
