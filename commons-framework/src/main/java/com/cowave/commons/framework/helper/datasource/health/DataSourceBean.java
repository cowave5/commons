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

import lombok.Data;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author shanhuiming
 *
 */
@Data
public class DataSourceBean {

    private DataSource dataSource;

    private DataSourceProperties dataSourceProperties;

    public boolean isEnable() {
        try(Connection ignored = dataSource.getConnection()){
            return true;
        }catch (SQLException e){
            return false;
        }
    }

    public String getUrl(){
        return dataSourceProperties.getUrl();
    }

    public String getUsername(){
        return dataSourceProperties.getUsername();
    }

    public String getDriver(){
        return dataSourceProperties.getDriverClassName();
    }
}
