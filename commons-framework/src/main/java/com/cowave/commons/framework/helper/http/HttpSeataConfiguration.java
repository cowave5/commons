/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.http;

import com.cowave.commons.client.http.HttpClientInterceptor;
import com.cowave.commons.client.http.HttpExceptionHandler;
import com.cowave.commons.client.http.HttpServiceChooser;
import com.cowave.commons.client.http.invoke.proxy.HttpMethodInvoker;
import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.framework.access.security.BearerTokenService;
import com.cowave.commons.framework.configuration.ApplicationProperties;
import com.cowave.commons.framework.helper.http.chooser.DefaultServiceChooser;
import com.cowave.commons.framework.helper.http.chooser.EurekaServiceChooser;
import com.cowave.commons.framework.helper.http.chooser.NacosServiceChooser;
import com.cowave.commons.framework.helper.http.exception.DefaultHttpExceptionHandler;
import com.cowave.commons.framework.helper.http.interceptor.HttpSeataInterceptor;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnClass({HttpMethodInvoker.class, RootContext.class})
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class HttpSeataConfiguration {

    @Nullable
    private final EurekaServiceChooser eurekaServiceChooser;

    @Nullable
    private final NacosServiceChooser nacosServiceChooser;

    @Nullable
    private final BearerTokenService bearerTokenService;

    @ConditionalOnMissingBean(HttpServiceChooser.class)
    @Bean
    public HttpServiceChooser httpServiceChooser(){
        return new DefaultServiceChooser(eurekaServiceChooser, nacosServiceChooser);
    }

    @Bean
    public HttpClientInterceptor httpClientInterceptor(
            @Value("${server.port:8080}") String port,
            ApplicationProperties applicationProperties, AccessProperties accessProperties) {
        return new HttpSeataInterceptor(port, bearerTokenService, accessProperties, applicationProperties);
    }

    @ConditionalOnMissingBean(HttpExceptionHandler.class)
    @Bean
    public DefaultHttpExceptionHandler httpExceptionHandler(){
        return new DefaultHttpExceptionHandler();
    }
}
