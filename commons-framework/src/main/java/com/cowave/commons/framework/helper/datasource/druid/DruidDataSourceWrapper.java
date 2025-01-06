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

import java.util.List;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnClass(DruidDataSource.class)
@Scope("prototype")
@Component
@ConfigurationProperties("spring.datasource.druid")
public class DruidDataSourceWrapper extends DruidDataSource {

    private final DataSourceProperties druidProperties;

    public DruidDataSourceWrapper(DataSourceProperties druidProperties) {
        this.druidProperties = druidProperties;
    }

    public DataSourceProperties getBasicProperties() {
        return druidProperties;
    }

    public void afterPropertiesSet() throws Exception {
        //if not found prefix 'spring.datasource.druid' jdbc properties ,'spring.datasource' prefix jdbc properties will be used.
        if (super.getUsername() == null) {
            super.setUsername(druidProperties.determineUsername());
        }
        if (super.getPassword() == null) {
            super.setPassword(druidProperties.determinePassword());
        }
        if (super.getUrl() == null) {
            super.setUrl(druidProperties.determineUrl());
        }
        if (super.getDriverClassName() == null) {
            super.setDriverClassName(druidProperties.getDriverClassName());
        }
    }

    @Autowired(required = false)
    public void autoAddFilters(List<Filter> filters) {
        super.filters.addAll(filters);
    }

    /**
     * Ignore the 'maxEvictableIdleTimeMillis &lt; minEvictableIdleTimeMillis' validate,
     * it will be validated again in {@link DruidDataSource#init()}.
     * for fix issue #3084, #2763
     *
     * @since 1.1.14
     */
    @Override
    public void setMaxEvictableIdleTimeMillis(long maxEvictableIdleTimeMillis) {
        try {
            super.setMaxEvictableIdleTimeMillis(maxEvictableIdleTimeMillis);
        } catch (IllegalArgumentException ignore) {
            super.maxEvictableIdleTimeMillis = maxEvictableIdleTimeMillis;
        }
    }
}
