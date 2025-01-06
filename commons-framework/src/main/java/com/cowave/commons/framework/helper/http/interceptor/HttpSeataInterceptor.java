/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.http.interceptor;

import com.cowave.commons.client.http.HttpClientInterceptor;
import com.cowave.commons.client.http.asserts.I18Messages;
import com.cowave.commons.client.http.request.HttpRequest;
import com.cowave.commons.framework.access.Access;
import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.framework.access.security.BearerTokenService;
import com.cowave.commons.framework.configuration.ApplicationProperties;
import com.cowave.commons.framework.helper.rest.interceptor.HeaderInterceptor;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
@RequiredArgsConstructor
public class HttpSeataInterceptor implements HttpClientInterceptor {

    private final String port;

    private final BearerTokenService bearerTokenService;

    private final AccessProperties accessProperties;

    private final ApplicationProperties applicationProperties;

    @Override
    public void apply(HttpRequest httpRequest) {
        // Header Access-Id
        String accessId = Access.accessId();
        if(StringUtils.isBlank(accessId)) {
            accessId = HeaderInterceptor.newAccessId(port, applicationProperties);
        }
        httpRequest.header("X-Request-ID", accessId);

        // Language
        httpRequest.header("Accept-Language", I18Messages.getLanguage().toLanguageTag());

        // Header Token
        if (!httpRequest.headers().containsKey(accessProperties.tokenKey())) {
            String authorization = Access.accessToken();
            if (StringUtils.isNotBlank(authorization)) {
                httpRequest.header(accessProperties.tokenKey(), authorization);
            } else if (bearerTokenService != null) {
                authorization = HeaderInterceptor.newAuthorization(bearerTokenService, applicationProperties);
                httpRequest.header(accessProperties.tokenKey(), authorization);
            }
        }

        // Header Seata事务id
        String xid = RootContext.getXID();
        if(StringUtils.isNotBlank(xid)){
            httpRequest.header(RootContext.KEY_XID, xid);
        }
    }
}
