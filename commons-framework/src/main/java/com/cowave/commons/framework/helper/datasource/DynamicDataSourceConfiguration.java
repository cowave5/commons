/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.datasource;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnMissingClass("com.alibaba.druid.pool.DruidDataSource")
@ConditionalOnClass(AbstractRoutingDataSource.class)
@EnableConfigurationProperties({DataSourceProperties.class, DynamicDataSourceProperties.class})
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@RequiredArgsConstructor
public class DynamicDataSourceConfiguration {

    private final DataSourceProperties dataSourceProperties;

    private final DynamicDataSourceProperties dynamicDataSourceProperties;

    @ConditionalOnMissingBean(DataSource.class)
    @Primary
    @Bean
    public DataSource dataSource() {
        Map<String, DataSourceProperties> propertiesMap = dynamicDataSourceProperties.getDynamic();
        if(propertiesMap != null && !propertiesMap.isEmpty()){
            DataSource primary = null;
            Map<Object, Object> dataSourceMap = new HashMap<>();
            for(Map.Entry<String, DataSourceProperties> entry : propertiesMap.entrySet()){
                DataSourceProperties properties = entry.getValue();
                DataSource dataSource = DataSourceBuilder.create()
                        .driverClassName(properties.getDriverClassName())
                        .type(properties.getType())
                        .url(properties.getUrl())
                        .username(properties.getUsername())
                        .password(properties.getPassword()).build();
                dataSourceMap.put(entry.getKey(), dataSource);
                if(Objects.equals(entry.getKey(), "primary")){
                    primary = dataSource;
                }
            }
            return new DynamicDataSource(primary, dataSourceMap);
        }else{
            DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
            if(dataSourceProperties.getType() != null){
                dataSourceBuilder.type(dataSourceProperties.getType());
            }
            if(dataSourceProperties.getDriverClassName() != null){
                dataSourceBuilder.driverClassName(dataSourceProperties.getDriverClassName());
            }
            if(dataSourceProperties.getUrl() != null){
                dataSourceBuilder.url(dataSourceProperties.getUrl());
            }
            if(dataSourceProperties.getUsername() != null){
                dataSourceBuilder.username(dataSourceProperties.getUsername());
            }
            if(dataSourceProperties.getPassword() != null){
                dataSourceBuilder.password(dataSourceProperties.getPassword());
            }
            DataSource dataSource = dataSourceBuilder.build();
            Map<Object, Object> dataSourceMap = new HashMap<>();
            dataSourceMap.put("primary", dataSource);
            return new DynamicDataSource(dataSource, dataSourceMap);
        }
    }
}
