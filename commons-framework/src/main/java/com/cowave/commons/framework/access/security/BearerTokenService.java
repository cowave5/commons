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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author shanhuiming
 */
public interface BearerTokenService {

    /**
     * 令牌类型
     */
    String CLAIM_TYPE = "Token.type";

    /**
     * 访问时间
     */
    String CLAIM_ACCESS_IP = "Token.ip";

    /**
     * Access令牌
     */
    String CLAIM_ACCESS_ID = "Token.access";

    /**
     * Refresh令牌
     */
    String CLAIM_REFRESH_ID = "Token.refresh";

    /**
     * 同一账号是否允许多客户端登录
     */
    String CLAIM_CONFLICT = "Token.conflict";

    /**
     * 租户id
     */
    String CLAIM_TENANT_ID = "Tenant.id";

    /**
     * 用户id
     */
    String CLAIM_USER_ID = "User.id";

    /**
     * 用户编号
     */
    String CLAIM_USER_CODE = "User.code";

    /**
     * 用户属性
     */
    String CLAIM_USER_PROPERTIES = "User.properties";

    /**
     * 用户类型
     */
    String CLAIM_USER_TYPE = "User.type";

    /**
     * 用户名称
     */
    String CLAIM_USER_NAME = "User.name";

    /**
     * 用户账号
     */
    String CLAIM_USER_ACCOUNT = "User.account";

    /**
     * 用户账号
     */
    String CLAIM_USER_ROLE = "User.role";

    /**
     * 用户权限
     */
    String CLAIM_USER_PERM = "User.permission";

    /**
     * 部门id
     */
    String CLAIM_DEPT_ID = "Dept.id";

    /**
     * 部门编号
     */
    String CLAIM_DEPT_CODE = "Dept.code";

    /**
     * 部门名称
     */
    String CLAIM_DEPT_NAME = "Dept.name";

    /**
     * 集群id
     */
    String CLAIM_CLUSTER_ID = "Cluster.id";

    /**
     * 集群级别
     */
    String CLAIM_CLUSTER_LEVEL = "Cluster.level";

    /**
     * 集群名称
     */
    String CLAIM_CLUSTER_NAME = "Cluster.name";

    /**
     * BearerTokenFilter解析AccessToken
     */
    AccessUserDetails parseAccessToken(HttpServletResponse response) throws IOException;

    /**
     * BearerTokenFilter解析AccessToken（使用RefreshToken）
     */
    AccessUserDetails parseAccessRefreshToken(HttpServletResponse response) throws IOException;

    /**
     * 授权设置AccessToken
     */
    void assignAccessToken(AccessUserDetails userDetails);

    /**
     * 授权设置AccessToken和RefreshToken
     */
    void assignAccessRefreshToken(AccessUserDetails userDetails);

    /**
     * 刷新AccessToken
     */
    String refreshAccessToken() throws Exception;

    /**
     * 刷新AccessToken和RefreshToken
     */
    AccessUserDetails refreshAccessRefreshToken(String refreshToken);

    /**
     * 注销AccessToken
     */
    AccessTokenInfo revokeAccessToken(String tenantId, String authType, String userAccount, String accessId);

    /**
     * 注销RefreshToken
     */
    RefreshTokenInfo revokeRefreshToken(String tenantId, String authType, String userAccount);

    /**
     * 验证AccessToken
     */
    boolean validAccessToken(String accessToken);

    /**
     * 获取AccessToken列表
     */
    List<AccessTokenInfo> listAccessToken(String tenantId);

    /**
     * 获取RefreshTokenInfo列表
     */
    List<RefreshTokenInfo> listRefreshToken(String tenantId);
}
