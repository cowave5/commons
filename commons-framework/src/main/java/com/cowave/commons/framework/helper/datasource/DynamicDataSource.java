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

import java.util.Map;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 *
 * @author shanhuiming
 *
 */
public class DynamicDataSource extends AbstractRoutingDataSource{

    private final Map<Object, Object> dataSourceMap;

    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources){
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
        this.dataSourceMap = targetDataSources;
    }

    public Map<Object, Object> getDataSourceMap() {
        return dataSourceMap;
    }

    @Override
    protected Object determineCurrentLookupKey(){
        return get();
    }

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    public static String get(){
        return HOLDER.get();
    }

    public static void set(String dataSourceName){
        HOLDER.set(dataSourceName);
    }

    public static void clear(){
        HOLDER.remove();
    }
}
