/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.rest;

import com.cowave.commons.client.http.request.ssl.NoopTlsSocketFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.net.SocketException;
import java.util.List;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(RestProperties.class)
public class RestConfiguration {

    private final RestProperties restProperties;

    @ConditionalOnMissingBean(RestTemplate.class)
    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper, HttpClient httpClient){
        RestTemplate restTemplate = new RestTemplate();
        // 请求处理
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        // 响应处理
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        for (int i = 0; i < messageConverters.size(); i++) {
            if (messageConverters.get(i) instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
                converter.setObjectMapper(objectMapper);
                messageConverters.set(i, converter);
            }
        }
        restTemplate.setMessageConverters(messageConverters);
        return restTemplate;
    }

    @ConditionalOnMissingBean(HttpClient.class)
    @Bean
    public HttpClient httpClient(SSLConnectionSocketFactory sslConnectionSocketFactory) {
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslConnectionSocketFactory).build();
        // 连接池
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        // 连接池最大连接数
        connectionManager.setMaxTotal(restProperties.getPoolConnections());
        // 路由是对maxTotal的细分
        connectionManager.setDefaultMaxPerRoute(500);
        // 返回数据等待时间
        connectionManager.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(restProperties.getSocketTimeout()).build());

        // 连接设置
        RequestConfig requestConfig = RequestConfig.custom()
                // 连接服务等待时间
                .setConnectTimeout(restProperties.getConnectTimeout())
                // 连接池获取连接等待时间
                .setConnectionRequestTimeout(restProperties.getPoolTimeout()).build();

        // 重试次数
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        httpClientBuilder.setConnectionManager(connectionManager);
        httpClientBuilder.setRetryHandler((exception, execCount, context) -> {
            if (execCount > restProperties.getRetryMax()) {
                return false;
            }

            try {
                Thread.sleep( execCount * restProperties.getRetryInterval());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            // io异常触发重试
            return exception instanceof ConnectTimeoutException
                    || exception instanceof NoHttpResponseException
                    || exception instanceof SocketException;
        });
        return httpClientBuilder.build();
    }

    @ConditionalOnMissingBean(SSLConnectionSocketFactory.class)
    @Bean
    public SSLConnectionSocketFactory sslConnectionSocketFactory(
            ObjectProvider<SSLSocketFactory> sslSocketFactoryProvider ,
            ObjectProvider<HostnameVerifier> hostnameVerifierProvider) throws Exception {
        SSLSocketFactory sslSocketFactory = sslSocketFactoryProvider.getIfAvailable();
        HostnameVerifier hostnameVerifier = hostnameVerifierProvider.getIfAvailable();
        if(sslSocketFactory == null){
            sslSocketFactory = new NoopTlsSocketFactory();
        }
        if(hostnameVerifier == null){
            hostnameVerifier = new NoopHostnameVerifier();
        }
        return new SSLConnectionSocketFactory(sslSocketFactory, hostnameVerifier);
    }
}
