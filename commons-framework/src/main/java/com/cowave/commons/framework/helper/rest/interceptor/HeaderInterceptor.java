/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.rest.interceptor;

import com.cowave.commons.framework.access.Access;
import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.framework.access.security.TokenService;
import com.cowave.commons.framework.configuration.ApplicationProperties;
import com.cowave.commons.framework.access.security.AccessToken;
import com.cowave.commons.tools.ids.IdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
@RequiredArgsConstructor
public class HeaderInterceptor implements ClientHttpRequestInterceptor {

    private static final IdGenerator GENERATOR = new IdGenerator();

    private final String port;

    private final TokenService tokenService;

    private final AccessProperties accessProperties;

    private final ApplicationProperties applicationProperties;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // Header Access-Id
        String accessId = Access.accessId();
        if(StringUtils.isBlank(accessId)) {
            accessId = newAccessId(port, applicationProperties);
            log.debug(">< new access-id: {}", accessId);
        }
        request.getHeaders().add("Access-Id", accessId);

        // Header Token
        String authorization = Access.accessToken();
        if(StringUtils.isNotBlank(authorization)){
            request.getHeaders().add(accessProperties.tokenHeader(), authorization);
        }
        if(tokenService != null){
            authorization = newAuthorization(tokenService, applicationProperties);
            request.getHeaders().add(accessProperties.tokenHeader(), authorization);
        }
        return execution.execute(request, body);
    }

    public static String newAccessId(String port, ApplicationProperties applicationProperties) {
        String prefix = "#" + applicationProperties.getClusterId() + port;
        return GENERATOR.generateIdWithDate(prefix, "", "yyyyMMddHHmmss", 1000);
    }

    public static String newAuthorization(TokenService tokenService, ApplicationProperties applicationProperties) {
        AccessToken appToken = AccessToken.newToken();
        appToken.setType(AccessToken.TYPE_APP);
        appToken.setUserId(-1L);
        appToken.setDeptId(-1L);
        appToken.setUsername(applicationProperties.getName());
        return tokenService.createAccessToken(appToken, 300);
    }
}
