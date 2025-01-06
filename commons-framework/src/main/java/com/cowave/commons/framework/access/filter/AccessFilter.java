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

import com.cowave.commons.client.http.asserts.I18Messages;
import com.cowave.commons.client.http.response.Response;
import com.cowave.commons.framework.access.Access;
import com.cowave.commons.framework.access.AccessLogger;
import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.tools.ServletUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.MimeHeaders;
import org.slf4j.MDC;
import org.springframework.http.MediaType;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;

import static com.cowave.commons.client.http.constants.HttpCode.BAD_REQUEST;
import static com.cowave.commons.client.http.constants.HttpCode.SUCCESS;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
public class AccessFilter implements Filter {

    private final TransactionIdSetter transactionIdSetter;

    private final AccessIdGenerator accessIdGenerator;

    private final AccessProperties accessProperties;

    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        // 获取accessId
        String accessId = httpServletRequest.getHeader("X-Request-ID");
        if (StringUtils.isBlank(accessId)) {
            accessId = accessIdGenerator.newAccessId();
        }
        // 获取国际化
        String language = httpServletRequest.getHeader("Accept-Language");
        // 获取Seata事务id
        String xid = httpServletRequest.getHeader("xid");

        // 设置响应头 Access-Id
        httpServletResponse.setHeader("X-Request-ID", accessId);
        // 设置响应头 Content-Security-Policy
        if(StringUtils.isNotBlank(accessProperties.getContentSecurityPolicy())){
            httpServletResponse.setHeader("Content-Security-Policy", accessProperties.getContentSecurityPolicy());
        }
        // 设置响应头 Access-Control
        AccessProperties.CrossControl crossControl = accessProperties.getCross();
        httpServletResponse.setHeader("Access-Control-Allow-Origin", crossControl.getAllowOrigin());
        httpServletResponse.setHeader("Access-Control-Allow-Methods", crossControl.getAllowMethods());
        httpServletResponse.setHeader("Access-Control-Allow-Headers", crossControl.getAllowHeaders());
        httpServletResponse.setHeader("Access-Control-Allow-Credentials", String.valueOf(crossControl.isAllowCredentials()));

        // 设置MDC.accessId
        MDC.put("accessId", accessId);
        // 设置国际化
        I18Messages.setLanguage(language);
        // 设置Seata事务id
        if(transactionIdSetter != null && xid != null){
            transactionIdSetter.setXid(xid);
        }
        // 设置Access
        String accessIp = ServletUtils.getRequestIp(httpServletRequest);
        String accessUrl = httpServletRequest.getRequestURI();
        String method = httpServletRequest.getMethod().toLowerCase();
        Access.set(new Access(true, accessId, accessIp, accessUrl, method, System.currentTimeMillis()));

        // 记录请求日志，顺便记录一下分页参数
        AccessRequestWrapper accessRequestWrapper = new AccessRequestWrapper(httpServletRequest, objectMapper);
        try{
            accessRequestWrapper.recordAccessParams();
        }catch (Exception e){
            int httpStatus = BAD_REQUEST.getStatus();
            if(accessProperties.isAlwaysSuccess()){
                httpStatus = SUCCESS.getStatus();
            }
            HttpServletResponse httpResponse = (HttpServletResponse)response;
            httpResponse.setCharacterEncoding("UTF-8");
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setStatus(httpStatus);
            response.getWriter().write(objectMapper.writeValueAsString(
                    Response.msg(BAD_REQUEST, I18Messages.msg("frame.advice.httpMessageConversionException"))));
            return;
        }

        // servlet处理
        chain.doFilter(accessRequestWrapper, response);

        // 拦截打印响应（AccessLogger中没有拦截到的）
        Access access = Access.get();
        if (!access.isResponseLogged()) {
            int status = httpServletResponse.getStatus();
            long cost = System.currentTimeMillis() - access.getAccessTime();
            if (status == SUCCESS.getStatus()) {
                AccessLogger.info("<< {} {}ms", status, cost);
            } else {
                if (!AccessLogger.isInfoEnabled()) {
                    AccessLogger.warn("<< {} {}ms {} {}", status, cost,
                            access.getAccessUrl(), objectMapper.writeValueAsString(access.getRequestParam()));
                }else{
                    AccessLogger.warn("<< {} {}ms", status, cost);
                }
            }
        }

        // 清除access
        Access.remove();
        MDC.remove("accessId");
    }

    public void headerAccessId(HttpServletRequest request, String value) {
        Class<?> clazz = request.getClass();
        try {
            Field req = clazz.getDeclaredField("request");
            req.setAccessible(true);
            Object o = req.get(request);

            Field coyoteRequest = o.getClass().getDeclaredField("coyoteRequest");
            coyoteRequest.setAccessible(true);
            Object oo = coyoteRequest.get(o);

            Field headers = oo.getClass().getDeclaredField("headers");
            headers.setAccessible(true);
            MimeHeaders mine = (MimeHeaders) headers.get(oo);
            mine.addValue("Access-Id").setString(value);
        } catch (Exception e) {
            // never will happened
            AccessLogger.error("", e);
        }
    }
}
