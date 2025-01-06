/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.filter;

import com.cowave.commons.framework.access.Access;
import com.cowave.commons.framework.access.AccessLogger;
import com.cowave.commons.tools.Converts;
import com.cowave.commons.tools.ServletUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.github.pagehelper.page.PageMethod;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 *
 * @author shanhuiming
 *
 */
public class AccessRequestWrapper extends HttpServletRequestWrapper {
    private static final String PAGE = "page";
    private static final String PAGE_INDEX = "pageIndex";
    private static final String PAGE_NO = "pageNo";
    private static final String PAGE_NUM = "pageNum";
    private static final String PAGE_NUMBER = "pageNumber";
    private static final String PAGE_SIZE = "pageSize";

    private String body = "";

    private final String contentType;

    private final ObjectMapper objectMapper;

    private final ObjectWriter objectWriter;

    public AccessRequestWrapper(HttpServletRequest request, ObjectMapper objectMapper) throws IOException {
        super(request);
        this.objectMapper = objectMapper;
        this.objectWriter = objectMapper.writer(new SimpleFilterProvider().addFilter(
                "passwdFilter", SimpleBeanPropertyFilter.serializeAllExcept("password", "passwd")));
        this.contentType = getContentType();
        setCharacterEncoding("UTF-8");
        setCharacterEncoding("UTF-8");
        if (StringUtils.startsWithIgnoreCase(contentType, MediaType.APPLICATION_JSON_VALUE)) {
            body = ServletUtils.getRequestBody(request);
        }
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public int available() {
                return bytes.length;
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }

    @SuppressWarnings("unchecked")
    public void recordAccessParams() throws JsonProcessingException {
        String url = getRequestURI();
        String remote = getRemoteAddr();

        // params
        Map<String, String> paramMap = new HashMap<>();
        Enumeration<String> parameterNames = getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = getParameterValues(paramName);
            if(paramValues.length > 1){
                paramMap.put(paramName, String.join(", ", paramValues));
            }else if(paramValues.length == 1){
                paramMap.put(paramName, paramValues[0]);
            }
        }

        // body
        Object bodyObject = null;
        if (StringUtils.isNotBlank(body)) {
            bodyObject = objectMapper.readValue(body, Object.class);
        }

        Map<String, Object> requestParams = new HashMap<>();
        // 请求日志
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append(">> ").append(getProtocol()).append(" ").append(getMethod()).append(" ").append(url);
        if(StringUtils.isNotBlank(contentType)){
            logBuilder.append(" ").append(contentType);
        }
        logBuilder.append(" ").append(remote);

        if(!paramMap.isEmpty()){
            requestParams.put("params", paramMap);
            logBuilder.append(" params=").append(objectWriter.writeValueAsString(paramMap));
        }
        if(bodyObject != null){
            requestParams.put("body", bodyObject);
            logBuilder.append(" body=").append(objectWriter.writeValueAsString(bodyObject));
        }
        AccessLogger.info(logBuilder.toString());

        // 记录请求参数
        Access access = Access.get();
        access.setRequestParam(requestParams);

        // 尝试获取分页参数
        Object index = getPageIndex(paramMap);
        if(index == null && bodyObject instanceof Map bodyMap){
            index = getPageIndex(bodyMap);
        }
        Object size = getPageSize(paramMap);
        if(size == null && bodyObject instanceof Map bodyMap){
            size = getPageSize(bodyMap);
        }

        // 记录分页参数
        Integer pageIndex = Converts.toInt(index, null);
        Integer pageSize = Converts.toInt(size, null);
        access.setPageIndex(pageIndex);
        access.setPageSize(pageSize);

        // 清除pageHelper设置
        PageMethod.clearPage();
    }

    private Object getPageIndex(Map<String, ?> paramMap){
        Object page = paramMap.get(PAGE);
        if(page == null){
            page = paramMap.get(PAGE_INDEX);
        }
        if(page == null){
            page = paramMap.get(PAGE_NO);
        }
        if(page == null){
            page = paramMap.get(PAGE_NUM);
        }
        if(page == null){
            page = paramMap.get(PAGE_NUMBER);
        }
        return page;
    }

    private Object getPageSize(Map<String, ?> paramMap){
        return paramMap.get(PAGE_SIZE);
    }
}

