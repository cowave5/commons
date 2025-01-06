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

import java.sql.*;

import com.cowave.commons.tools.SpringContext;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;


/**
 *
 * @author shanhuiming
 *
 */
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private final Class<T> clazz;

    public JsonTypeHandler(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.clazz = clazz;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        DatabaseProvider dynamic = SpringContext.getBean("databaseIdProvider");
        if("postgres".equals(dynamic.getCurrentDatabase())){
            PGobject pgObject = new PGobject();
            pgObject.setType("json");
            pgObject.setValue(this.toJson(parameter));
            ps.setObject(i, pgObject);
        }else{
            ps.setObject(i, this.toJson(parameter));
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return this.toObject(rs.getString(columnName), clazz);
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return this.toObject(rs.getString(columnIndex), clazz);
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return this.toObject(cs.getString(columnIndex), clazz);
    }

    private String toJson(T object) {
        try {
            return JSON.toJSONString(object, SerializerFeature.WriteNullListAsEmpty);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private T toObject(String content, Class<?> clazz) {
        if (content != null && !content.isEmpty()) {
            try {
                return (T) JSON.parseObject(content,clazz);
            } catch (Exception e) {
                content = content.substring(1, content.length()-1).replace("\\\"", "\"");
                return (T) JSON.parseObject(content,clazz);
            }
        } else {
            return null;
        }
    }
}
