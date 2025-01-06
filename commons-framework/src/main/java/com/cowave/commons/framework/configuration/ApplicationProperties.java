/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.configuration;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
@Data
@ConfigurationProperties(prefix = "spring.application")
public class ApplicationProperties {

    /**
     * 应用名称
     */
    private String name;

    /**
     * 应用版本
     */
    private String version;

    /**
     * 命名空间 Redis缓存
     */
    private String namespace;

    /**
     * 集群id
     */
    private int clusterId = 10;

    /**
     * 集群等级
     */
    private int clusterLevel = 1;

    /**
     * 集群名称
     */
    private String clusterName = "default";

    /**
     * 集群属性
     */
    private Map<String, Object> clusterProperties;

    public String getTokenNamespace(){
        if(StringUtils.isBlank(namespace)){
            return "token:";
        }
        if(namespace.endsWith(":")){
            return namespace + "token:";
        }else{
            return namespace + ":token:";
        }
    }

    public String getLimitNamespace(){
        if(StringUtils.isBlank(namespace)){
            return "limit:";
        }
        if(namespace.endsWith(":")){
            return namespace + "limit:";
        }else{
            return namespace + ":limit:";
        }
    }

    public String getCacheNamespace(){
        if(StringUtils.isBlank(namespace)){
            return "cache:";
        }
        if(namespace.endsWith(":")){
            return namespace + "cache:";
        }else{
            return namespace + ":cache:";
        }
    }

    public String getDictNamespace(){
        if(StringUtils.isBlank(namespace)){
            return "dict:";
        }
        if(namespace.endsWith(":")){
            return namespace + "dict:";
        }else{
            return namespace + ":dict:";
        }
    }

    public String getLockNamespace(){
        if(StringUtils.isBlank(namespace)){
            return "lock:";
        }
        if(namespace.endsWith(":")){
            return namespace + "lock:";
        }else{
            return namespace + ":lock:";
        }
    }
}
