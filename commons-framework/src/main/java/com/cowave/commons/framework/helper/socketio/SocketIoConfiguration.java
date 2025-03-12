/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.socketio;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.framework.access.security.BearerTokenService;
import com.cowave.commons.framework.helper.socketio.listener.SocketIoAuthorizationListener;
import com.cowave.commons.framework.helper.socketio.listener.SocketIoExceptionListener;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnProperty(name = "spring.socket-io.enable", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(SocketIOServer.class)
@EnableConfigurationProperties(SocketIoProperties.class)
@RequiredArgsConstructor
@Configuration
public class SocketIoConfiguration {

    @Nullable
    private final BearerTokenService bearerTokenService;

    @Nullable
    private final SocketIoEventRegister socketIoEventRegister;

    @Bean
    public ExceptionListener exceptionListener(){
        return new SocketIoExceptionListener();
    }

    @Bean
    public AuthorizationListener authorizationListener(AccessProperties accessProperties){
        return new SocketIoAuthorizationListener(accessProperties, bearerTokenService);
    }

    @Bean
    public SocketIOServer socketIoServer(SocketIoProperties properties,
                                         ExceptionListener exceptionListener,
                                         AuthorizationListener authorizationListener) {
        com.corundumstudio.socketio.Configuration configuration = new com.corundumstudio.socketio.Configuration();
        if (properties.getHost() != null) {
            configuration.setHostname(properties.getHost());
        }
        configuration.setPort(properties.getPort());
        configuration.setContext(properties.getContextPath());
        configuration.setBossThreads(properties.getBossCount());
        configuration.setWorkerThreads(properties.getWorkCount());
        configuration.setUpgradeTimeout(properties.getUpgradeTimeout());
        configuration.setPingTimeout(properties.getPingTimeout());
        configuration.setPingInterval(properties.getPingInterval());
        configuration.setAllowCustomRequests(properties.isAllowCustomRequests());
        configuration.setMaxHttpContentLength(properties.getMaxHttpContentLength());
        configuration.setMaxFramePayloadLength(properties.getMaxFramePayloadLength());

        configuration.setExceptionListener(exceptionListener);
        configuration.setAuthorizationListener(authorizationListener);

        SocketConfig socket = new SocketConfig();
        socket.setSoLinger(0);
        socket.setTcpNoDelay(true);
        configuration.setSocketConfig(socket);
        return new SocketIOServer(configuration);
    }

    @Bean
    public SocketIoHelper socketIoHelper(SocketIoProperties properties, SocketIOServer socketIoServer){
        return new SocketIoHelper(properties, socketIoServer, socketIoEventRegister);
    }

    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketIoServer) {
        return new SpringAnnotationScanner(socketIoServer);
    }
}
