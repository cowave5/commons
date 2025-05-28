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
import com.cowave.commons.framework.access.security.AccessUserDetails;
import com.cowave.commons.tools.ServletUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.MimeHeaders;
import org.slf4j.MDC;
import org.springframework.http.MediaType;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.cowave.commons.client.http.constants.HttpCode.BAD_REQUEST;
import static com.cowave.commons.client.http.constants.HttpCode.SUCCESS;
import static com.cowave.commons.client.http.constants.HttpHeader.*;
import static com.cowave.commons.framework.access.security.BearerTokenService.*;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
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
        // X-Request-ID
        String accessId = httpServletRequest.getHeader(X_Request_ID);
        if (StringUtils.isBlank(accessId)) {
            accessId = accessIdGenerator.newAccessId();
        }
        // Accept-Language
        String language = httpServletRequest.getHeader(Accept_Language);
        // 获取Seata事务id
        String xid = httpServletRequest.getHeader("xid");

        // 设置响应头
        httpServletResponse.setHeader(X_Request_ID, accessId);
        // 设置响应头 Content-Security-Policy
        if(StringUtils.isNotBlank(accessProperties.getContentSecurityPolicy())){
            httpServletResponse.setHeader(Content_Security_Policy, accessProperties.getContentSecurityPolicy());
        }
        // 设置响应头 Access-Control
        AccessProperties.CrossControl crossControl = accessProperties.getCross();
        httpServletResponse.setHeader(Access_Control_Allow_Origin, crossControl.getAllowOrigin());
        httpServletResponse.setHeader(Access_Control_Allow_Methods, crossControl.getAllowMethods());
        httpServletResponse.setHeader(Access_Control_Allow_Headers, crossControl.getAllowHeaders());
        httpServletResponse.setHeader(Access_Control_Allow_Credentials, String.valueOf(crossControl.isAllowCredentials()));

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

        Access access = new Access(true, accessId, accessIp, accessUrl, method, System.currentTimeMillis());
        parseUserPayload(access, httpServletRequest);
        Access.set(access);

        // 请求参数、日志
        AccessRequestWrapper accessRequestWrapper = new AccessRequestWrapper(httpServletRequest, objectMapper, access);
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

        // 拦截打印响应（如果AccessLogger中没有打印）
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

    private void parseUserPayload(Access access, HttpServletRequest httpServletRequest){
        String userPayload = httpServletRequest.getHeader(X_User_Payload);
        if(StringUtils.isBlank(userPayload)){
            return;
        }

        String json = new String(Base64.getUrlDecoder().decode(userPayload));
        try {
            Map<String, Object> claims = objectMapper.readValue(json, Map.class);

            AccessUserDetails userDetails = new AccessUserDetails();
            userDetails.setType((String) claims.get(CLAIM_TYPE));
            userDetails.setAccessId((String) claims.get(CLAIM_ACCESS_ID));
            userDetails.setRefreshId((String) claims.get(CLAIM_REFRESH_ID));
            // user
            userDetails.setUserId(claims.get(CLAIM_USER_ID));
            userDetails.setUserCode(claims.get(CLAIM_USER_CODE));
            userDetails.setUsername((String) claims.get(CLAIM_USER_ACCOUNT));
            userDetails.setUserNick((String) claims.get(CLAIM_USER_NAME));
            userDetails.setUserProperties((Map<String, Object>) claims.get(CLAIM_USER_PROPERTIES));
            // dept
            userDetails.setDeptId(claims.get(CLAIM_DEPT_ID));
            userDetails.setDeptCode(claims.get(CLAIM_DEPT_CODE));
            userDetails.setDeptName((String) claims.get(CLAIM_DEPT_NAME));
            // cluster
            userDetails.setClusterId((Integer) claims.get(CLAIM_CLUSTER_ID));
            userDetails.setClusterLevel((Integer) claims.get(CLAIM_CLUSTER_LEVEL));
            userDetails.setClusterName((String) claims.get(CLAIM_CLUSTER_NAME));
            // roles
            userDetails.setRoles((List<String>) claims.get(CLAIM_USER_ROLE));
            // permits
            userDetails.setPermissions((List<String>) claims.get(CLAIM_USER_PERM));
            // 设置
            access.setUserDetails(userDetails);
        } catch (JsonProcessingException e) {
            log.error("", e);
        }
    }

    private void headerAccessId(HttpServletRequest request, String value) {
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
