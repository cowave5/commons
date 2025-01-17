/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.feign;

import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.framework.access.security.BearerTokenService;
import com.cowave.commons.framework.configuration.ApplicationProperties;
import com.cowave.commons.framework.helper.feign.chooser.DefaultServiceChooser;
import com.cowave.commons.framework.helper.feign.chooser.EurekaServiceChooser;
import com.cowave.commons.framework.helper.feign.chooser.NacosServiceChooser;
import com.cowave.commons.framework.helper.feign.interceptor.FeignHeaderInterceptor;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.feign.FeignServiceChooser;
import org.springframework.feign.annotation.EnableFeign;
import org.springframework.feign.invoke.FeignSyncInvoker;

import javax.annotation.Nullable;

/**
 *
 * @author shanhuiming
 *
 */
@EnableFeign
@ComponentScan(basePackages = "com.cowave")
@ConditionalOnMissingClass("io.seata.core.context.RootContext")
@ConditionalOnClass({FeignSyncInvoker.class})
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class FeignConfiguration {

    @Nullable
    private final EurekaServiceChooser eurekaServiceChooser;

    @Nullable
    private final NacosServiceChooser nacosServiceChooser;

    @Nullable
    private final BearerTokenService bearerTokenService;

    @ConditionalOnMissingBean(FeignServiceChooser.class)
    @Bean
    public FeignServiceChooser feignServiceChooser(){
        return new DefaultServiceChooser(eurekaServiceChooser, nacosServiceChooser);
    }

    @Bean
    public RequestInterceptor requestInterceptor(@Value("${server.port:8080}") String port,
            ApplicationProperties applicationProperties, AccessProperties accessProperties) {
        return new FeignHeaderInterceptor(port, bearerTokenService, accessProperties, applicationProperties);
    }
}
