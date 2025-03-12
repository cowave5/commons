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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
@RequiredArgsConstructor
public class SocketIoHelper {

    static Map<String, Map<String, SocketIOClient>> namespcaeClientMap = new ConcurrentHashMap<>();

    static Map<String, SocketIOClient> rootClientMap = new ConcurrentHashMap<>();

    private final SocketIoProperties properties;

    private final SocketIOServer socketIoServer;

    private final SocketIoEventRegister socketIoEventRegister;

    @PostConstruct
    private void init() {
        // netty-socketIo设计的感觉不太友好，本身不支持区分clientId，需要自己维护
        // 对于namespace，服务端需要事先声明Namespace，否则会走触发默认的ConnectListener以及DisconnectListener
        List<String> namespaces = properties.getNamespaces();
        if (CollectionUtils.isEmpty(namespaces)) {
            // 连接
            socketIoServer.addConnectListener(client -> {
                String clientId = client.getHandshakeData().getSingleUrlParam("clientId");
                if (clientId != null) {
                    SocketIoHelper.rootClientMap.put(clientId, client);
                }
            });

            // 断开
            socketIoServer.addDisconnectListener(client -> {
                String clientId = client.getHandshakeData().getSingleUrlParam("clientId");
                if (clientId != null) {
                    SocketIoHelper.rootClientMap.remove(clientId);
                }
            });

            // event
            if (socketIoEventRegister != null) {
                socketIoEventRegister.registerEventListener(socketIoServer);
            }
        } else {
            for (String namespace : namespaces) {
                // 连接
                socketIoServer.addNamespace(namespace).addConnectListener(client -> {
                    Map<String, SocketIOClient> clientMap =
                            SocketIoHelper.namespcaeClientMap.computeIfAbsent(namespace, k -> new HashMap<>());
                    String clientId = client.getHandshakeData().getSingleUrlParam("clientId");
                    if (clientId != null) {
                        clientMap.put(clientId, client);
                    }
                });

                // 断开
                socketIoServer.addNamespace(namespace).addDisconnectListener(client -> {
                    Map<String, SocketIOClient> clientMap = SocketIoHelper.namespcaeClientMap.get(namespace);
                    if (clientMap != null) {
                        String clientId = client.getHandshakeData().getSingleUrlParam("clientId");
                        if (clientId != null) {
                            clientMap.remove(clientId);
                        }
                    }
                });

                // event
                if (socketIoEventRegister != null) {
                    socketIoEventRegister.registerEventListener(socketIoServer);
                }
            }
        }
        socketIoServer.start();
    }

    @PreDestroy
    private void destroy() {
        if (socketIoServer != null) {
            socketIoServer.stop();
        }
    }

    /**
     * 发送数据
     * @param event 事件
     * @param data 数据
     */
    public <T> void send(String event, T data){
        for(SocketIOClient client : rootClientMap.values()){
            client.sendEvent(event, data);
        }
    }

    /**
     * 发送数据到指定namespace
     * @param namespace 命名空间
     * @param event 事件
     * @param data 数据
     */
    public <T> void sendWithNamespace(String namespace, String event, T data) {
        if(StringUtils.isBlank(namespace)){
            return;
        }
        Map<String, SocketIOClient> clientMap = namespcaeClientMap.get(namespace);
        if (clientMap != null) {
            for (SocketIOClient client : clientMap.values()) {
                client.sendEvent(event, data);
            }
        }
    }

    /**
     * 发送数据到room
     * @param room 房间
     * @param event 事件
     * @param data 数据
     */
    public <T> void sendRoom(String room, String event, T data){
        if(StringUtils.isBlank(room)){
            return;
        }
        for(SocketIOClient client : rootClientMap.values()){
            client.joinRoom(room);
            client.sendEvent(event, data);
        }
    }

    /**
     * 发送数据到指定namespace中的room
     * @param namespace 命名空间
     * @param room 房间
     * @param event 事件
     * @param data 数据
     */
    public <T> void sendRoomWithNamespace(String namespace, String room, String event, T data) {
        if(StringUtils.isBlank(namespace) || StringUtils.isBlank(room)){
            return;
        }
        Map<String, SocketIOClient> clientMap = namespcaeClientMap.get(namespace);
        if (clientMap != null) {
            for (SocketIOClient client : clientMap.values()) {
                client.joinRoom(room);
                client.sendEvent(event, data);
            }
        }
    }

    /**
     * 发送数据到客户端
     * @param clientIds 客户端id
     * @param event 事件
     * @param data 数据
     */
    public <T> void sendClients(Collection<String> clientIds, String event, T data){
        if(CollectionUtils.isEmpty(clientIds)){
            return;
        }
        for(String clientId : clientIds){
            SocketIOClient client = rootClientMap.get(clientId);
            if(client != null){
                client.sendEvent(event, data);
            }
        }
    }

    /**
     * 发送数据到指定namespace下的客户端
     * @param namespace 命名空间
     * @param clientIds 客户端id
     * @param data 数据
     * @param event 事件
     */
    public <T> void sendClientsWithNamespace(String namespace, Collection<String> clientIds, String event, T data) {
        if (StringUtils.isBlank(namespace) || CollectionUtils.isEmpty(clientIds)) {
            return;
        }
        Map<String, SocketIOClient> clientMap = namespcaeClientMap.get(namespace);
        if (clientMap != null) {
            for (String clientId : clientIds) {
                SocketIOClient client = clientMap.get(clientId);
                if (client != null) {
                    client.sendEvent(event, data);
                }
            }
        }
    }

    /**
     * 发送数据到room中的客户端
     * @param room 房间
     * @param clientIds 客户端id
     * @param data 数据
     * @param event 事件
     */
    public <T> void sendClientsInRoom(String room, Collection<String> clientIds, String event, T data){
        if (StringUtils.isBlank(room) || CollectionUtils.isEmpty(clientIds)) {
            return;
        }
        for(String clientId : clientIds){
            SocketIOClient client = rootClientMap.get(clientId);
            if(client != null){
                client.joinRoom(room);
                client.sendEvent(event, data);
            }
        }
    }

    /**
     * 发送数据到指定namespace下room中的客户端
     * @param namespace 命名空间
     * @param room 房间
     * @param clientIds 客户端id
     * @param data 数据
     * @param event 事件
     */
    public <T> void sendClientsInRoomWithNamespace(String namespace, String room, Collection<String> clientIds, String event, T data) {
        if (StringUtils.isBlank(namespace) || StringUtils.isBlank(room) || CollectionUtils.isEmpty(clientIds)) {
            return;
        }
        Map<String, SocketIOClient> clientMap = namespcaeClientMap.get(namespace);
        if (clientMap != null) {
            for (String clientId : clientIds) {
                SocketIOClient client = clientMap.get(clientId);
                if (client != null) {
                    client.joinRoom(room);
                    client.sendEvent(event, data);
                }
            }
        }
    }
}
