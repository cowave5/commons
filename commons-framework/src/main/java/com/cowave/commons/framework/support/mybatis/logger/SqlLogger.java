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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SqlLogger implements Interceptor {

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 设置配置插件参数
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long sTime = System.currentTimeMillis();
        Object proceed = invocation.proceed();
        long cost = System.currentTimeMillis() - sTime;

        try {
            log.info("[{}ms]: {}", cost, getSql(invocation));
        }catch (Exception e){
            log.error("sql logger failed", e);
        }
        return proceed;
    }

    private String getSql(Invocation invocation){
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = null;
        if (invocation.getArgs().length>1){
            parameter = invocation.getArgs()[1];
        }
        // BoundSql存储一条具体的SQL及其相关参数
        // Configuration保存了MyBatis运行时所有的配置
        Configuration configuration = statement.getConfiguration();
        BoundSql boundSql = statement.getBoundSql(parameter);

        // 参数对象
        Object params = boundSql.getParameterObject();
        // 参数映射
        List<ParameterMapping> paramMappings = boundSql.getParameterMappings();
        // 执行SQL
        String sql = boundSql.getSql();
        // 多空格替换
        sql = sql.replaceAll("[\\s]+", " ");

        if (!ObjectUtils.isEmpty(paramMappings) && !ObjectUtils.isEmpty(params)){
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            // 如果参数对象的类型有对应的TypeHandler，则用TypeHandler进行处理
            if (typeHandlerRegistry.hasTypeHandler(params.getClass())){
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(params)));
            }else {
                // 否则逐个处理参数映射
                for (ParameterMapping param : paramMappings) {
                    // 参数属性名
                    String propertyName = param.getProperty();
                    MetaObject metaObject = configuration.newMetaObject(params);
                    // 检查对象中getter方法，如果存在就取出来进行替换
                    if (metaObject.hasGetter(propertyName)){
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    }else if (boundSql.hasAdditionalParameter(propertyName)){
                        // 检查BoundSql中是否存在附加参数，可能是在动态SQL处理中生成的，有的话就进行替换
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    }else {
                        // 如果都没有，说明SQL匹配不上
                        sql = sql.replaceFirst("\\?", "<unknown>");
                    }
                }
            }
        }
        return sql;
    }

    private static String getParameterValue(Object object) {
        String value = "";
        if (object instanceof String){
            value = "'" + object + "'";
        }else if (object instanceof Date){
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + format.format((Date) object) + "'";
        } else if (!ObjectUtils.isEmpty(object)) {
            value = object.toString();
        }
        return value;
    }
}
