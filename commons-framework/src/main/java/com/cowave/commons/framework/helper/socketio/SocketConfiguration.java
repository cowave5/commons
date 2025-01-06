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

import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.framework.access.security.BearerTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nullable;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnClass(SocketIOServer.class)
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(SocketIoProperties.class)
public class SocketConfiguration {

    @Nullable
    private final BearerTokenService bearerTokenService;

    @Nullable
    private final ClientMsgHandler clientMsgHandler;

    @Nullable
    private final ConnectedHandler connectedHandler;

    private final AccessProperties accessProperties;

    @Bean
    public SocketIoHelper socketServer(SocketIoProperties properties) {
        com.corundumstudio.socketio.Configuration configuration = new com.corundumstudio.socketio.Configuration();
        if(properties.getHost() != null){
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
        if(bearerTokenService != null) {
            configuration.setAuthorizationListener(data -> {
                String authorization = data.getSingleUrlParam(accessProperties.tokenKey());
                return bearerTokenService.validAccessJwt(authorization);
            });
        }

        SocketConfig socket = new SocketConfig();
        socket.setTcpNoDelay(true);
        socket.setSoLinger(0);
        configuration.setSocketConfig(socket);
        return new SocketIoHelper(clientMsgHandler, connectedHandler, new SocketIOServer(configuration));
    }
}
