/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.mybatis.logger;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnProperty("logging.sql-enabled")
@ConditionalOnClass(MybatisAutoConfiguration.class)
@AutoConfigureAfter(MybatisAutoConfiguration.class)
@RequiredArgsConstructor
public class SqlLoggerMybatisConfiguration implements InitializingBean {

    private final List<SqlSessionFactory> sqlSessionFactoryList;

    @Override
    public void afterPropertiesSet() {
        SqlLogger interceptor = new SqlLogger();
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
            if (!containsInterceptor(configuration, interceptor)) {
                configuration.addInterceptor(interceptor);
            }
        }
    }

    private boolean containsInterceptor(org.apache.ibatis.session.Configuration configuration, Interceptor sqlInterceptor) {
        return configuration.getInterceptors().stream().anyMatch(interceptor -> sqlInterceptor.getClass().isAssignableFrom(interceptor.getClass()));
    }
}
