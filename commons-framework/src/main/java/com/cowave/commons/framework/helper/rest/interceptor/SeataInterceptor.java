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
import com.cowave.commons.response.exception.Messages;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
@RequiredArgsConstructor
public class SeataInterceptor implements ClientHttpRequestInterceptor {

    private final String port;

    private final ApplicationProperties applicationProperties;

    private final AccessProperties accessProperties;

    private final TokenService tokenService;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // Header X-Request-ID
        String accessId = Access.accessId();
        if(StringUtils.isBlank(accessId)) {
            accessId = HeaderInterceptor.newAccessId(port, applicationProperties);
            log.debug(">< new access-id: {}", accessId);
        }
        request.getHeaders().add("X-Request-ID", accessId);

        // Header Accept-Language
        request.getHeaders().add("Accept-Language", Messages.getLanguage().getLanguage());

        // Header Token
        if (!request.getHeaders().containsKey(accessProperties.tokenHeader())) {
            String authorization = Access.accessToken();
            if (StringUtils.isNotBlank(authorization)) {
                request.getHeaders().add(accessProperties.tokenHeader(), authorization);
            } else if (tokenService != null) {
                authorization = HeaderInterceptor.newAuthorization(tokenService, applicationProperties);
                request.getHeaders().add(accessProperties.tokenHeader(), authorization);
            }
        }

        // Header Seata事务id
        String xid = RootContext.getXID();
        if(StringUtils.isNotBlank(xid)){
            request.getHeaders().add(RootContext.KEY_XID, xid);
        }
        return execution.execute(request, body);
    }
}