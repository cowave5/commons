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

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
@NoArgsConstructor
@Data
public class RefreshTokenInfo {

    /**
     * Access Token id
     */
    private String accessId;

    /**
     * Refresh Token id
     */
    private String refreshId;

    /**
     * 类型
     */
    private String authType;

    /**
     * 租户id
     */
    private String tenantId;

    /**
     * 用户id
     */
    private Object userId;

    /**
     * 用户code
     */
    private Object userCode;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户属性
     */
    private Map<String, Object> userProperties;

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
     * 登录iP
     */
    private String loginIp;

    /**
     * 登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date loginTime;

    public RefreshTokenInfo(AccessUserDetails accessUserDetails){
        this.accessId = accessUserDetails.getAccessId();
        this.refreshId = accessUserDetails.getRefreshId();
        this.authType = accessUserDetails.getAuthType();
        this.tenantId = accessUserDetails.getTenantId();
        this.userId = accessUserDetails.getUserId();
        this.userCode = accessUserDetails.getUserCode();
        this.userAccount = accessUserDetails.getUsername();
        this.userName = accessUserDetails.getUserNick();
        this.userProperties = accessUserDetails.getUserProperties();
        this.deptId = accessUserDetails.getDeptId();
        this.deptCode = accessUserDetails.getDeptCode();
        this.deptName = accessUserDetails.getDeptName();
        this.clusterId = accessUserDetails.getClusterId();
        this.clusterLevel = accessUserDetails.getClusterLevel();
        this.clusterName = accessUserDetails.getClusterName();
        this.roles = accessUserDetails.getRoles();
        this.permissions = accessUserDetails.getPermissions();
        this.loginIp = accessUserDetails.getLoginIp();
        this.loginTime = accessUserDetails.getLoginTime();
    }
}
