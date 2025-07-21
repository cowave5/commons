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
import com.cowave.commons.framework.configuration.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static com.cowave.commons.framework.access.security.AuthMode.ACCESS;
import static com.cowave.commons.framework.access.security.AuthMode.ACCESS_REFRESH;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
public class AccessUserDetailsServiceImpl implements TenantUserDetailsService {

    private final AuthMode authMode;

    private final ApplicationProperties applicationProperties;

    private final PasswordEncoder passwordEncoder;

    private final BearerTokenService bearerTokenService;

    private final Map<String, AccessUser> userMap;

    @Override
    public AccessUserDetails loadTenantUserByUsername(String tenantId, String username) throws UsernameNotFoundException {
        AccessUser accessUser = userMap.get(username);
        if(accessUser == null){
            throw new UsernameNotFoundException(username + " not exist");
        }

        AccessUserDetails accessUserDetails = AccessUserDetails.newUserDetails();
        accessUserDetails.setAccessId(IdUtil.fastSimpleUUID());
        accessUserDetails.setRefreshId(IdUtil.fastSimpleUUID());
        accessUserDetails.setAuthType("default");
        accessUserDetails.setUserId(accessUser.getUserId());
        accessUserDetails.setUserCode(accessUser.getUserCode());
        accessUserDetails.setUserNick(accessUser.getUserNick());
        accessUserDetails.setUsername(accessUser.getUsername());
        accessUserDetails.setUserPasswd(passwordEncoder.encode(accessUser.getPassword()));
        accessUserDetails.setUserProperties(accessUser.getUserProperties());
        accessUserDetails.setRoles(accessUser.getRoles());
        accessUserDetails.setPermissions(accessUser.getPermissions());
        accessUserDetails.setDeptId(accessUser.getDeptId());
        accessUserDetails.setDeptCode(accessUser.getDeptCode());
        accessUserDetails.setDeptName(accessUser.getDeptName());
        accessUserDetails.setClusterId(applicationProperties.getClusterId());
        accessUserDetails.setClusterLevel(applicationProperties.getClusterLevel());
        accessUserDetails.setClusterName(applicationProperties.getClusterName());
        if (ACCESS == authMode) {
            bearerTokenService.assignAccessToken(accessUserDetails);
        } else if (ACCESS_REFRESH == authMode) {
            bearerTokenService.assignAccessRefreshToken(accessUserDetails);
        }
        return accessUserDetails;
    }
}
