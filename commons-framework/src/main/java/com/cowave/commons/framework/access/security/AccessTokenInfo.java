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

/**
 * @author shanhuiming
 */
@NoArgsConstructor
@Data
public class AccessTokenInfo {

    /**
     * 令牌id
     */
    private String accessId;

    /**
     * Refresh Token id
     */
    private String refreshId;

    /**
     * 令牌类型
     */
    private String accessType;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 访问IP
     */
    private String accessIp;

    /**
     * 访问集群
     */
    private String accessCluster;

    /**
     * 访问时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date accessTime;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date loginTime;

    public AccessTokenInfo(AccessUserDetails accessUserDetails) {
        this.accessId = accessUserDetails.getAccessId();
        this.refreshId = accessUserDetails.getRefreshId();
        this.accessType = accessUserDetails.getAuthType();
        this.userAccount = accessUserDetails.getUsername();
        this.userName = accessUserDetails.getUserNick();
        this.accessIp = accessUserDetails.getAccessIp();
        this.accessTime = accessUserDetails.getAccessTime();
        this.loginIp = accessUserDetails.getLoginIp();
        this.loginTime = accessUserDetails.getLoginTime();
        this.accessCluster = accessUserDetails.getClusterName();
    }
}
