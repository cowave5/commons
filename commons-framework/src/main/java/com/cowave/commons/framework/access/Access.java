/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import cn.hutool.core.net.NetUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cowave.commons.framework.access.filter.AccessIdGenerator;
import com.cowave.commons.framework.access.security.AccessInfo;
import com.cowave.commons.framework.access.security.AccessUserDetails;
import com.cowave.commons.tools.ServletUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author shanhuiming
 *
 */
@Data
public class Access {

    private static final ThreadLocal<Access> ACCESS = new TransmittableThreadLocal<>();

    private final boolean accessFiltered;

    private final String accessId;

    private final String accessIp;

    private final String accessUrl;

    private final String accessMethod;

    private final Long accessTime;

    private AccessUserDetails userDetails;

    private Integer pageIndex;

    private Integer pageSize;

    private boolean responseLogged;

    private Map<String, Object> requestParam = new HashMap<>();

    public Access(boolean accessFiltered, String accessId, String accessIp, String accessUrl, String accessMethod, Long accessTime){
        this.accessFiltered = accessFiltered;
        this.accessId = accessId;
        this.accessIp = accessIp;
        this.accessUrl = accessUrl;
        this.accessMethod = accessMethod;
        this.accessTime = accessTime;
    }

    public static Access get(){
        return ACCESS.get();
    }

    public static void set(Access access){
        ACCESS.set(access);
    }

    public static Access newAccess(AccessIdGenerator accessIdGenerator){
        String accessId = null;
        String accessIp = null;
        String accessUrl = null;
        String accessMethod = null;
        HttpServletRequest httpServletRequest = httpRequest();
        if(httpServletRequest != null){
            accessId = httpServletRequest.getHeader("X-Request-ID");
            accessIp = ServletUtils.getRequestIp(httpServletRequest);
            accessUrl = httpServletRequest.getRequestURI();
            accessMethod = httpServletRequest.getMethod().toLowerCase();
        }
        if (StringUtils.isBlank(accessId)) {
            accessId = accessIdGenerator.newAccessId();
        }
        return new Access(false, accessId, accessIp, accessUrl, accessMethod, System.currentTimeMillis());
    }

    public static void remove(){
        ACCESS.remove();
    }

    public static String accessId() {
        return Optional.ofNullable(get()).map(access -> access.accessId).orElse(null);
    }

    public static String accessIp() {
        return Optional.ofNullable(get()).map(access -> access.accessIp).orElse(NetUtil.getLocalhostStr());
    }

    public static String accessUrl() {
        return Optional.ofNullable(get()).map(access -> access.accessUrl).orElse(null);
    }

    public static String accessMethod() {
        return Optional.ofNullable(get()).map(access -> access.accessMethod).orElse(null);
    }

    public static Date accessTime() {
        return Optional.ofNullable(get()).map(access -> new Date(access.accessTime)).orElse(new Date());
    }

    public static <T> Page<T> page(){
        return Optional.ofNullable(get()).map(
                access -> new Page<T>(
                        Optional.ofNullable(access.pageIndex).orElse(1),
                        Optional.ofNullable(access.pageSize).orElse(10)
                )).orElse(new Page<>());
    }

    public static <T> Page<T> page(int defaultSize){
        return Optional.ofNullable(get()).map(
                access -> new Page<T>(
                    Optional.ofNullable(access.pageIndex).orElse(1),
                    Optional.ofNullable(access.pageSize).orElse(defaultSize)
            )).orElse(new Page<>(1, defaultSize));
    }

    public static int pageIndex() {
        return Optional.ofNullable(get()).map(access -> Optional.ofNullable(access.pageIndex).orElse(1)).orElse(1);
    }

    public static int pageSize() {
        return Optional.ofNullable(get()).map(access -> Optional.ofNullable(access.pageSize).orElse(10)).orElse(10);
    }

    public static int pageSize(int defaultSize) {
        return Optional.ofNullable(get()).map(access -> Optional.ofNullable(access.pageSize).orElse(defaultSize)).orElse(defaultSize);
    }

    public static int pageOffset() {
        return Optional.ofNullable(get()).map(access -> {
                int pageIndex = Optional.ofNullable(access.pageIndex).orElse(1);
                int pageSize = Optional.ofNullable(access.pageSize).orElse(10);
                return (pageIndex <= 0 || pageSize <= 0) ? 0 : (pageIndex - 1) * pageSize;
            }).orElse(0);
    }

    public static int pageOffset(int defaultSize) {
        return Optional.ofNullable(get()).map(access -> {
                int pageIndex = Optional.ofNullable(access.pageIndex).orElse(1);
                int pageSize = Optional.ofNullable(access.pageSize).orElse(defaultSize);
                return (pageIndex <= 0 || pageSize <= 0) ? 0 : (pageIndex - 1) * pageSize;
            }).orElse(0);
    }

    public static AccessUserDetails userDetails() {
        return Optional.ofNullable(get()).map(access -> access.userDetails).orElse(null);
    }

    public static String tokenType() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getType).orElse(null);
    }

    public static String accessToken() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getAccessToken).orElse(null);
    }

    public static String refreshToken() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getRefreshToken).orElse(null);
    }

    public static AccessInfo accessInfo(){
        return new AccessInfo(userDetails());
    }

    public static <T> T userId() {
        return (T) Optional.ofNullable(userDetails()).map(AccessUserDetails::getUserId).orElse(null);
    }

    public static <T> T userId(Function<Object, T> converter) {
        return Optional.ofNullable(userDetails()).map(
                userDetails -> converter.apply(userDetails.getUserId())).orElse(null);
    }

    public static <T> T userCode() {
        return (T) Optional.ofNullable(userDetails()).map(AccessUserDetails::getUserCode).orElse(null);
    }

    public static <T> T userCode(Function<Object, T> converter) {
        return Optional.ofNullable(userDetails()).map(
                userDetails -> converter.apply(userDetails.getUserCode())).orElse(null);
    }

    public static String userName() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getUserNick).orElse(null);
    }

    public static String userAccount() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getUsername).orElse(null);
    }

    public static List<String> userRoles() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getRoles).orElse(new ArrayList<>());
    }

    public static List<String> userPermissions() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getPermissions).orElse(new ArrayList<>());
    }

    public static boolean userIsAdmin(){
        return Optional.of(userRoles()).filter(roles -> !roles.isEmpty()).map(roles -> roles.contains("sysAdmin")).orElse(false);
    }

    public static <T> T deptId() {
        return (T) Optional.ofNullable(userDetails()).map(AccessUserDetails::getDeptId).orElse(null);
    }

    public static <T> T deptId(Function<Object, T> converter) {
        return Optional.ofNullable(userDetails()).map(
                userDetails -> converter.apply(userDetails.getDeptId())).orElse(null);
    }

    public static <T> T deptCode() {
        return (T) Optional.ofNullable(userDetails()).map(AccessUserDetails::getDeptCode).orElse(null);
    }

    public static <T> T deptCode(Function<Object, T> converter) {
        return Optional.ofNullable(userDetails()).map(
                userDetails -> converter.apply(userDetails.getDeptCode())).orElse(null);
    }

    public static String deptName() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getDeptName).orElse(null);
    }

    public static Integer clusterId() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getClusterId).orElse(null);
    }

    public static Integer clusterLevel() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getClusterLevel).orElse(null);
    }

    public static String clusterName() {
        return Optional.ofNullable(userDetails()).map(AccessUserDetails::getClusterName).orElse(null);
    }

    public static HttpServletRequest httpRequest() {
        return Optional.ofNullable((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .map(ServletRequestAttributes::getRequest).orElse(null);
    }

    public static HttpServletResponse httpResponse() {
        return Optional.ofNullable((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .map(ServletRequestAttributes::getResponse).orElse(null);
    }

    public static Map<String, Object> getRequestParam() {
        return Optional.ofNullable(get()).map(access -> access.requestParam).orElse(null);
    }

    public static String getRequestHeader(String headerName) {
        return Optional.ofNullable(httpRequest()).map(httpRequest -> httpRequest.getHeader(headerName)).orElse(null);
    }

    public static void setResponseStatus(HttpStatus httpStatus){
        Optional.ofNullable(httpResponse()).ifPresent(response -> response.setStatus(httpStatus.value()));
    }

    public static void setResponseHeader(String name, String value){
        Optional.ofNullable(httpResponse()).ifPresent(response -> response.setHeader(name, value));
    }

    public static HttpSession httpSession() {
        return Optional.ofNullable(httpRequest()).map(HttpServletRequest::getSession).orElse(null);
    }

    public static Cookie[] httpCookies(){
        return Optional.ofNullable(httpRequest()).map(HttpServletRequest::getCookies).orElse(null);
    }

    public static String getCookie(String name) {
        return Optional.ofNullable(httpCookies()).flatMap(
                cookies -> Arrays.stream(cookies).filter(cookie -> cookie.getName().equals(name)).findFirst())
                .map(cookie -> URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8)).orElse(null);
    }

    public static void setCookie(String name, String value) {
        setCookie(name, value, "/");
    }

    public static void setCookie(String name, String value, String path) {
        setCookie(name, value, path, 3600 * 24);
    }

    public static void setCookie(String name, String value, String path, int age) {
        Optional.ofNullable(httpResponse()).ifPresent(response -> {
            Cookie cookie = new Cookie(name, URLEncoder.encode(value, StandardCharsets.UTF_8));
            cookie.setPath(path);
            cookie.setMaxAge(age);
            response.addCookie(cookie);
        });
    }

    public static void removeCookie(String name) {
        Optional.ofNullable(httpResponse()).ifPresent(
                response -> Optional.ofNullable(httpCookies()).ifPresent(
                        cookies -> Arrays.stream(cookies).filter(cookie -> cookie.getName().equals(name)).forEach(
                                cookie -> {
                                    cookie.setMaxAge(0);
                                    response.addCookie(cookie);
                                })));
    }
}
