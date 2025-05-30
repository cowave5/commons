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

import com.cowave.commons.framework.configuration.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnMissingClass("io.seata.core.context.RootContext")
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class HeaderInterceptorConfiguration {

    @Bean
    public ClientHttpRequestInterceptor clientHttpRequestInterceptor(
            @Value("${server.port:8080}") String port, ApplicationProperties applicationProperties) {
        return new HeaderInterceptor(port, applicationProperties);
    }
}
