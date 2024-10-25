/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import lombok.Data;
import org.springframework.feign.annotation.EnableFeign;

import java.util.List;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
@Data
@EnableFeign
@EnableAspectJAutoProxy(exposeProxy = true)
@ComponentScan(basePackages = "com.cowave")
@ConfigurationProperties(prefix = "spring.application")
public class ApplicationConfiguration implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private String name;

    private String version;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String yamlPath = "/META-INF/common.yml";
        Resource infoResource = new ClassPathResource(yamlPath);
        if(!infoResource.exists()){
            return;
        }

        try{
            log.info("prepare to load: META-INF/common.yml");
            List<PropertySource<?>> list = new YamlPropertySourceLoader().load(yamlPath, infoResource);
            for(PropertySource<?> source : list){
                environment.getPropertySources().addLast(source);
            }
        }catch (Exception e){
            log.error("failed to load: META-INF/common.yml", e);
            System.exit(-1);
        }
    }
}
