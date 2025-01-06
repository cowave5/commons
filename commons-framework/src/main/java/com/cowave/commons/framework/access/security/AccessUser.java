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

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
@Data
public class AccessUser {

    /**
     * 用户id
     */
    private Object userId;

    /**
     * 用户code
     */
    private Object userCode;

    /**
     * 用户昵称
     */
    private String userNick;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 用户角色
     */
    private List<String> roles;

    /**
     * 用户权限
     */
    private List<String> permissions;

    /**
     * 用户属性
     */
    private Map<String, Object> userProperties;

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

    public static AccessUser defaultUser(){
        AccessUser accessUser = new AccessUser();
        accessUser.setUsername("cowave");
        accessUser.setPassword("Cowave@123");
        accessUser.setRoles(List.of("sysAdmin"));
        accessUser.setPermissions(List.of("*"));
        return accessUser;
    }
}
