/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.feign.interceptor;

import com.cowave.commons.framework.configuration.ApplicationConfiguration;
import com.cowave.commons.framework.configuration.ClusterInfo;
import com.cowave.commons.framework.filter.security.TokenService;
import feign.RequestInterceptor;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.feign.annotation.FeignClient;

import javax.annotation.Nullable;

/**
*
* @author shanhuiming
*
*/
@ConditionalOnClass({FeignClient.class, RootContext.class})
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class TransactionIdInterceptorConfiguration {

    @Nullable
    private final TokenService tokenService;

    @Bean
    public RequestInterceptor requestInterceptor(
            ApplicationConfiguration applicationConfiguration, ClusterInfo clusterInfo) {
        return new TransactionIdInterceptor(applicationConfiguration, tokenService, clusterInfo);
    }
}
