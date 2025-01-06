/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.datasource.health;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
public class DataSourceHealthIndicator extends AbstractHealthIndicator {

    private final DataSourceBean dataSourceBean;

    public DataSourceHealthIndicator(DataSourceBean dataSourceBean) {
        this.dataSourceBean = dataSourceBean;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if(dataSourceBean.isEnable()){
            builder.up();
        }else{
            builder.down();
        }
        Map<String, String> info = new HashMap<>();
        info.put("url", dataSourceBean.getUrl());
        info.put("username", dataSourceBean.getUsername());
        info.put("driver", dataSourceBean.getDriver());
        builder.withDetails(info);
    }
}
