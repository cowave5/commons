/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.filter;

import com.cowave.commons.framework.access.AccessProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.annotation.Nullable;

/**
 *
 * @author shanhuiming
 *
 */
@Data
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(AccessProperties.class)
public class AccessFilterConfiguration {

    private final ObjectMapper objectMapper;

    @Nullable
    private final TransactionIdSetter transactionIdSetter;

    @Bean
    public AccessIdGenerator accessIdGenerator(@Value("${spring.application.cluster-id:10}${server.port:8080}") String idPrefix){
        return new AccessIdGenerator(idPrefix);
    }

    @Bean
    public FilterRegistrationBean<AccessFilter> accessFilterRegistration(AccessIdGenerator accessIdGenerator, AccessProperties accessProperties){
        FilterRegistrationBean<AccessFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AccessFilter(transactionIdSetter, accessIdGenerator, accessProperties, objectMapper));
        registration.setName("accessFilter");
        registration.addUrlPatterns(accessProperties.getUrlPatterns());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
