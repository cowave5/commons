/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
@Data
@ConfigurationProperties(prefix = "spring.cache")
public class CacheProperties {

    /**
     * 打印过程日志
     */
    private boolean logEnable;

    /**
     * L1 本地缓存配置
     */
    private L1Properties l1 = new L1Properties();

    /**
     * L2 Redis缓存配置，区分Key
     */
    private Map<Object, L2Properties> l2 = new HashMap<>();

    public boolean l1Enable(){
        return l1.enable;
    }

    public long l1ExpireAfterAccess(){
        return l1.expireAfterAccess;
    }

    public long l1ExpireAfterWrite(){
        return l1.expireAfterWrite;
    }

    public long l1RefreshAfterWrite(){
        return l1.refreshAfterWrite;
    }

    public int l1InitialCapacity(){
        return l1.initialCapacity;
    }

    public long l1MaximumSize(){
        return l1.maximumSize;
    }

    public boolean l2Enable(Object cacheKey){
        L2Properties l2Properties = l2.get(cacheKey);
        if(l2Properties != null){
            return l2Properties.enable;
        }
        return true;
    }

    public boolean l2First(Object cacheKey){
        L2Properties l2Properties = l2.get(cacheKey);
        if(l2Properties != null){
            return l2Properties.first;
        }
        return false;
    }

    public int l2ExpireAfterAccess(String cacheKey){
        L2Properties l2Properties = l2.get(cacheKey);
        if(l2Properties != null){
            return l2Properties.expireAfterAccess;
        }
        return 60;
    }

    public int l2ExpireAfterWrite(String cacheKey){
        L2Properties l2Properties = l2.get(cacheKey);
        if(l2Properties != null){
            return l2Properties.expireAfterWrite;
        }
        return 60;
    }

    @Data
    public static class L1Properties {

        /**
         * 启用一级缓存
         */
        private boolean enable = true;

        /**
         * 访问后过期时间，单位秒
         */
        private long expireAfterAccess = -1;

        /**
         * 写入后过期时间，单位秒
         */
        private long expireAfterWrite = -1;

        /**
         * 写入后刷新时间，单位秒
         */
        private long refreshAfterWrite = -1;

        /**
         * 初始化大小
         */
        private int initialCapacity = -1;

        /**
         * 最大缓存对象个数，超过此数量时之前放入的缓存将失效
         */
        private long maximumSize = -1;

    }

    @Data
    public static class L2Properties {

        /**
         * 启用二级缓存
         */
        private boolean enable = true;

        /**
         * 优先二级缓存
         */
        private boolean first = false;

        /**
         * 访问后过期时间，单位秒
         */
        private int expireAfterAccess = 60;

        /**
         * 写入后过期时间，单位秒
         */
        private int expireAfterWrite = 60;
    }
}
