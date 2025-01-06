/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.mybatis;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>MYSQL("mysql", "MySql数据库"),
 * <p>MARIADB("mariadb", "MariaDB数据库"),
 * <p>ORACLE("oracle", "Oracle11g及以下数据库(高版本推荐使用ORACLE_NEW)"),
 * <p>ORACLE_12C("oracle12c", "Oracle12c+数据库"),
 * <p>DB2("db2", "DB2数据库"),
 * <p>H2("h2", "H2数据库"),
 * <p>HSQL("hsql", "HSQL数据库"),
 * <p>SQLITE("sqlite", "SQLite数据库"),
 * <p>POSTGRE_SQL("postgresql", "Postgre数据库"),
 * <p>SQL_SERVER2005("sqlserver2005", "SQLServer2005数据库"),
 * <p>SQL_SERVER("sqlserver", "SQLServer数据库"),
 * <p>DM("dm", "达梦数据库"),
 * <p>XU_GU("xugu", "虚谷数据库"),
 * <p>KINGBASE_ES("kingbasees", "人大金仓数据库"),
 * <p>PHOENIX("phoenix", "Phoenix HBase数据库"),
 * <p>GAUSS("zenith", "Gauss 数据库"),
 * <p>CLICK_HOUSE("clickhouse", "clickhouse 数据库"),
 * <p>GBASE("gbase", "南大通用(华库)数据库"),
 * <p>GBASE_8S("gbase-8s", "南大通用数据库 GBase 8s"),
 * <p>GBASEDBT("gbasedbt", "南大通用数据库"),
 * <p>GBASE_INFORMIX("gbase 8s", "南大通用数据库 GBase 8s"),
 * <p>OSCAR("oscar", "神通数据库"),
 * <p>SYBASE("sybase", "Sybase ASE 数据库"),
 * <p>OCEAN_BASE("oceanbase", "OceanBase 数据库"),
 * <p>FIREBIRD("Firebird", "Firebird 数据库"),
 * <p>HIGH_GO("highgo", "瀚高数据库"),
 * <p>CUBRID("cubrid", "CUBRID数据库"),
 * <p>GOLDILOCKS("goldilocks", "GOLDILOCKS数据库"),
 * <p>CSIIDB("csiidb", "CSIIDB数据库"),
 * <p>SAP_HANA("hana", "SAP_HANA数据库"),
 * <p>IMPALA("impala", "impala数据库"),
 * <p>VERTICA("vertica", "vertica数据库"),
 * <p>XCloud("xcloud", "行云数据库"),
 * <p>OTHER("other", "其他数据库");
 *
 * @see com.baomidou.mybatisplus.annotation.DbType
 *
 * @author shanhuiming
 */
@ConditionalOnClass(DatabaseIdProvider.class)
@Component("databaseIdProvider")
public class DatabaseProvider implements DatabaseIdProvider {

    private static final String KEY_DB_TYPE = "DB_TYPE";

    private static final Map<String, String> DB_PRODUCT = new HashMap<>();

    private final ConcurrentHashMap<String, String> currentDb = new ConcurrentHashMap<>();

    static {
        DB_PRODUCT.put("H2", "H2");
        DB_PRODUCT.put("OSCAR", "oscar");
        DB_PRODUCT.put("MySQL", "mysql");
        DB_PRODUCT.put("Oracle", "oracle");
        DB_PRODUCT.put("PostgreSQL", "postgres");
        DB_PRODUCT.put("KingbaseES", "postgres");
    }

    @Override
    public String getDatabaseId(DataSource dataSource) throws SQLException {
        try(Connection connection = dataSource.getConnection()){
            String productName = connection.getMetaData().getDatabaseProductName();
            String databaseId = DB_PRODUCT.get(productName);
            if(databaseId == null){
                currentDb.put(KEY_DB_TYPE, productName);
            }else{
                currentDb.put(KEY_DB_TYPE, databaseId);
            }
            return databaseId;
        }
    }

    public String getCurrentDatabase(){
        return currentDb.get(KEY_DB_TYPE);
    }
}
