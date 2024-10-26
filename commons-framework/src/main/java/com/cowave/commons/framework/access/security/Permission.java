/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.security;

import java.util.List;

import com.cowave.commons.framework.access.Access;
import com.cowave.commons.framework.configuration.ApplicationProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author shanhuiming
 *
 */
@SuppressWarnings("deprecation")
@RequiredArgsConstructor
@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
@Component("permit")
public class Permission {

    public static final String ROLE_ADMIN = "sysAdmin";

    public static final String PERMIT_ADMIN = "*:*:*";

    private final ApplicationProperties applicationProperties;

    public boolean isAdmin() {
        List<String> roles = Access.userRoles();
        if(CollectionUtils.isEmpty(roles)) {
            return false;
        }
        return roles.contains(ROLE_ADMIN);
    }

    public boolean hasPermit(String permission) {
        if(isAdmin()) {
            return true;
        }

        List<String> perms = Access.userPermissions();
        if(CollectionUtils.isEmpty(perms)) {
            return false;
        }
        return perms.contains(permission) || perms.contains(PERMIT_ADMIN);
    }

    public boolean hasRole(String role) {
        if(isAdmin()) {
            return true;
        }
        List<String> roles = Access.userRoles();
        if(CollectionUtils.isEmpty(roles)) {
            return false;
        }
        return roles.contains(role);
    }

    public boolean isCurrentCluster() {
        Integer clusterId = Access.clusterId();
    	return clusterId != null && Objects.equal(clusterId, applicationProperties.getClusterId());
    }
}