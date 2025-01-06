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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author shanhuiming
 *
 */
@Data
@ConfigurationProperties("spring.rest")
public class RestProperties {

    /**
     * 连接超时
     */
    private int connectTimeout = 10000;

    /**
     * 响应超时
     */
    private int socketTimeout = 120000;

    /**
     * 获取连接超时
     */
    private int poolTimeout = 30000;

    /**
     * 最大连接数
     */
    private int poolConnections = 100;

    /**
     * 最大重试次数
     */
    private int retryMax = 3;

    /**
     * 重试间隔
     */
    private long retryInterval = 1000;
}
