/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.security;

import cn.hutool.core.util.IdUtil;
import com.cowave.commons.framework.access.Access;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.*;
import java.util.function.Function;

/**
 *
 * @author shanhuiming
 *
 */
@Data
public class AccessUserDetails implements UserDetails {

    @Serial
    private static final long serialVersionUID = -3928832861296252415L;

    public static final String TYPE_APP = "app";

    public static final String TYPE_USER = "user";

    public static final String TYPE_LDAP = "ldap";

    public static final String TYPE_OAUTH = "oauth";

    /**
     * accessToken
     */
    private String accessToken;

    /**
     * refreshToken
     */
    private String refreshToken;

    /**
     * id (Refresh时的随机id，可以判断Token是否已被refresh过）
     */
    private String id;

    /**
     * type
     */
    private String type;

    /**
     * 登录iP
     */
    private String loginIp;

    /**
     * 登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginTime;

    /**
     * 访问ip
     */
    private String accessIp;

    /**
     * 访问时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date accessTime;

    /**
     * 用户id
     */
    private Object userId;

    /**
     * 用户code
     */
    private Object userCode;

    /**
     * 用户属性
     */
    private Map<String, Object> userProperties;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 用户昵称
     */
    private String userNick;

    /**
     * 用户密码
     */
    @JsonIgnore
    private String userPasswd;

    /**
     * 用户Roles
     */
    private List<String> roles;

    /**
     * 用户Permissions
     */
    private List<String> permissions;

    /**
     * 部门id
     */
    private Object deptId;

    /**
     * 部门code
     */
    private Object deptCode;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 集群id
     */
    private Integer clusterId;

    /**
     * 集群level
     */
    private Integer clusterLevel;

    /**
     * 集群name
     */
    private String clusterName;

    /**
     * 权限
     */
    private List<? extends GrantedAuthority> authorities;

    public AccessUserDetails(){

    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return userPasswd;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return true;
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(authorities != null){
            return authorities;
        }
        if(CollectionUtils.isNotEmpty(roles)){
            List<SimpleGrantedAuthority> list = new ArrayList<>(roles.size());
            for(String role : roles){
                list.add(new SimpleGrantedAuthority(role));
            }
            return list;
        }
        return new ArrayList<>();
    }

    public static AccessUserDetails newUserDetails(){
        AccessUserDetails accessUserDetails = new AccessUserDetails();
        accessUserDetails.setLoginIp(Access.accessIp());
        accessUserDetails.setLoginTime(Access.accessTime());
        accessUserDetails.setAccessIp(Access.accessIp());
        accessUserDetails.setAccessTime(Access.accessTime());
        accessUserDetails.setId(IdUtil.fastSimpleUUID());
        return accessUserDetails;
    }

    public <T> T getUserId(){
        return (T)userId;
    }

    public <T> T getUserId(Function<Object, T> converter) {
        return converter.apply(userId);
    }

    public <T> T getUserCode(){
        return (T)userCode;
    }

    public <T> T getUserCode(Function<Object, T> converter) {
        return converter.apply(userCode);
    }

    public <T> T getDeptId(){
        return (T)deptId;
    }

    public <T> T getDeptId(Function<Object, T> converter) {
        return converter.apply(deptId);
    }

    public <T> T getDeptCode(){
        return (T)deptCode;
    }

    public <T> T getDeptCode(Function<Object, T> converter) {
        return converter.apply(deptCode);
    }
}
