/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.rest.interceptor;

import com.cowave.commons.client.http.asserts.I18Messages;
import com.cowave.commons.framework.access.Access;
import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.framework.access.security.BearerTokenService;
import com.cowave.commons.framework.configuration.ApplicationProperties;
import com.cowave.commons.framework.access.security.AccessUserDetails;
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

    private final ApplicationProperties applicationProperties;

    private final AccessProperties accessProperties;

    private final BearerTokenService bearerTokenService;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // Header X-Request-ID
        String accessId = Access.accessId();
        if(StringUtils.isBlank(accessId)) {
            accessId = newAccessId(port, applicationProperties);
            log.debug(">< new access-id: {}", accessId);
        }
        request.getHeaders().add("X-Request-ID", accessId);

        // Header Accept-Language
        request.getHeaders().add("Accept-Language", I18Messages.getLanguage().getLanguage());

        // Header Token
        if (!request.getHeaders().containsKey(accessProperties.tokenKey())) {
            String authorization = Access.accessToken();
            if (StringUtils.isNotBlank(authorization)) {
                request.getHeaders().add(accessProperties.tokenKey(), authorization);
            } else if (bearerTokenService != null) {
                authorization = newAuthorization(bearerTokenService, applicationProperties);
                request.getHeaders().add(accessProperties.tokenKey(), authorization);
            }
        }
        return execution.execute(request, body);
    }

    public static String newAccessId(String port, ApplicationProperties applicationProperties) {
        String prefix = "#" + applicationProperties.getClusterId() + port;
        return GENERATOR.generateIdWithDate(prefix, "", "yyyyMMddHHmmss", 1000);
    }

    public static String newAuthorization(BearerTokenService bearerTokenService, ApplicationProperties applicationProperties) {
        AccessUserDetails userDetails = AccessUserDetails.newUserDetails();
        userDetails.setUsername(applicationProperties.getName());
        return "Bearer " + bearerTokenService.newApiAccessToken(userDetails, 300);
    }
}
