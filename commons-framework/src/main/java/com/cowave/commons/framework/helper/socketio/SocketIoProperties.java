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

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

import java.util.List;

/**
 * @author shanhuiming
 */
@Data
@ConfigurationProperties(prefix = "spring.socket-io")
public class SocketIoProperties {

    private boolean allowCustomRequests = true;

    private List<String> namespaces;

    private String host;

    /**
     * socket端口，默认复用Http端口
     */
    private Integer port;

    /**
     * socket路径
     */
    private String contextPath = "/socket.io";

    /**
     * 每帧处理数据的最大长度，防止他人利用大数据来攻击服务器
     */
    private int maxFramePayloadLength = 1048576;

    /**
     * http交互最大内容长度
     */
    private int maxHttpContentLength = 1048576;

    /**
     * HTTP握手升级为ws协议超时时间，默认10秒
     */
    private int upgradeTimeout = 1000000;

    /**
     * 客户端向服务器发送心跳周期
     */
    private int pingInterval = 25000;

    /**
     * 间隔内没有接收到心跳就会发送超时事件
     */
    private int pingTimeout = 6000000;

    private int bossCount = 1;

    private int workCount = 100;
}
