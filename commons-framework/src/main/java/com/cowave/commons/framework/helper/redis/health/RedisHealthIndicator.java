/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis.health;

import java.util.*;

import com.cowave.commons.framework.helper.redis.StringRedisHelper;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 *
 * @author shanhuiming
 *
 */
public class RedisHealthIndicator extends AbstractHealthIndicator {

    private final StringRedisHelper redis;

    public RedisHealthIndicator(StringRedisHelper redis) {
        this.redis = redis;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try{
            String pong = redis.ping();
            if("PONG".equals(pong)){
                builder.up();
            }else{
                builder.down();
            }

            Properties info = redis.info();
            assert info != null;
            RedisConnectionFactory connectionFactory = redis.getRedisTemplate().getConnectionFactory();

            assert connectionFactory != null;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("redis_version", info.get("redis_version"));
            if("standalone".equals(info.get("redis_mode"))){
                map.put("redis_mode", info.get("redis_mode"));
                map.put("redis_port", info.get("tcp_port"));
                if(connectionFactory instanceof LettuceConnectionFactory lettuce){
                    map.put("redis_host", lettuce.getHostName());
                }

            }else if("sentinel".equals(info.get("redis_mode"))){
                map.put("redis_mode", info.get("sentinel"));
                if(connectionFactory instanceof LettuceConnectionFactory lettuce){
                    RedisConfiguration.SentinelConfiguration sentinel = lettuce.getSentinelConfiguration();
                    if(sentinel != null){
                        NamedNode master = sentinel.getMaster();
                        if(master != null){
                            map.put("master", master.getName());
                        }

                        Set<RedisNode> nodes = sentinel.getSentinels();
                        StringBuilder build = new StringBuilder();
                        for(RedisNode node : nodes){
                            build.append(node.getHost()).append(":").append(node.getPort()).append(",");
                        }
                        map.put("nodes", build.subSequence(0, build.length() - 1));
                    }
                }

            }else if("cluster".equals(info.get("redis_mode"))) {
                map.put("redis_mode", info.get("cluster"));
                if(connectionFactory instanceof LettuceConnectionFactory lettuce){
                    RedisConfiguration.ClusterConfiguration cluster = lettuce.getClusterConfiguration();
                    if(cluster != null){
                        Set<RedisNode> nodes = cluster.getClusterNodes();
                        StringBuilder build = new StringBuilder();
                        for (RedisNode node : nodes) {
                            build.append(node.getHost()).append(":").append(node.getPort()).append(",");
                        }
                        map.put("nodes", build.subSequence(0, build.length() - 1));
                    }
                }
            }
            map.put("memory_used", info.get("used_memory_human"));
            map.put("connected_clients", info.get("connected_clients"));
            map.put("uptime_in_days", info.get("uptime_in_days"));
            builder.withDetails(map);
        }catch (Exception e){
            builder.down();
        }
    }
}
