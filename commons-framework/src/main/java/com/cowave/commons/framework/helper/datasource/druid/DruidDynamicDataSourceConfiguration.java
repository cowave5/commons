/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.datasource.druid;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.stat.DruidFilterConfiguration;
import com.alibaba.druid.spring.boot.autoconfigure.stat.DruidSpringAopConfiguration;
import com.alibaba.druid.spring.boot.autoconfigure.stat.DruidStatViewServletConfiguration;
import com.alibaba.druid.spring.boot.autoconfigure.stat.DruidWebStatFilterConfiguration;
import com.cowave.commons.framework.helper.datasource.DynamicDataSourceProperties;
import com.cowave.commons.framework.helper.datasource.DynamicDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.alibaba.druid.spring.boot.autoconfigure.properties.DruidStatProperties;
import org.springframework.context.annotation.Primary;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnClass(DruidSpringAopConfiguration.class)
@Import({DruidSpringAopConfiguration.class, DruidStatViewServletConfiguration.class,
        DruidWebStatFilterConfiguration.class, DruidFilterConfiguration.class})
@EnableConfigurationProperties({DataSourceProperties.class, DynamicDataSourceProperties.class, DruidStatProperties.class})
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@RequiredArgsConstructor
public class DruidDynamicDataSourceConfiguration {

    private final DataSourceProperties dataSourceProperties;

    private final DynamicDataSourceProperties dynamicDataSourceProperties;

    private final ApplicationContext applicationContext;

    @ConditionalOnMissingBean(DataSource.class)
    @Primary
    @Bean
    public DataSource dataSource() throws Exception {
        Map<String, DataSourceProperties> propertiesMap = dynamicDataSourceProperties.getDynamic();
        if(propertiesMap != null && !propertiesMap.isEmpty()){
            DruidDataSource primary = null;
            Map<Object, Object> dataSourceMap = new HashMap<>();
            for(Map.Entry<String, DataSourceProperties> entry : propertiesMap.entrySet()){
                DruidDataSourceWrapper dataSource = applicationContext.getBean(DruidDataSourceWrapper.class, entry.getValue());
                dataSource.afterPropertiesSet();
                dataSource.init();
                dataSourceMap.put(entry.getKey(), dataSource);
                if(Objects.equals(entry.getKey(), "primary")){
                    primary = dataSource;
                }
            }
            return new DynamicDataSource(primary, dataSourceMap);
        }else{
            DruidDataSourceWrapper dataSource = applicationContext.getBean(DruidDataSourceWrapper.class, dataSourceProperties);
            dataSource.afterPropertiesSet();
            dataSource.init();
            Map<Object, Object> dataSourceMap = new HashMap<>();
            dataSourceMap.put("primary", dataSource);
            return new DynamicDataSource(dataSource, dataSourceMap);
        }
    }
}
