/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis.connection;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.JedisClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.Jedis;

/**
 *
 * @author shanhuiming
 *
 */
@AutoConfigureBefore({LettuceAutoConfiguration.class})
@ConditionalOnProperty(name = "spring.redis.client-type", havingValue = "jedis", matchIfMissing = true)
@ConditionalOnClass({ GenericObjectPool.class, JedisConnection.class, Jedis.class })
@EnableConfigurationProperties({RedisProperties.class})
public class JedisAutoConfiguration {

    @ConditionalOnMissingBean(AbstractRedisConnectionConfiguration.class)
    @Primary
    @Bean
    public JedisRedisConnectionConfiguration redisConnectionConfiguration(RedisProperties redisProperties,
            ObjectProvider<RedisSentinelConfiguration> sentinelConfiguration,
            ObjectProvider<RedisClusterConfiguration> clusterConfiguration){
        return new JedisRedisConnectionConfiguration(redisProperties, sentinelConfiguration, clusterConfiguration);
    }

    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    @Primary
    @Bean
    public JedisConnectionFactory redisConnectionFactory(
            JedisRedisConnectionConfiguration redisConnectionConfiguration,
            ObjectProvider<JedisClientConfigurationBuilderCustomizer> builderCustomizers){
        return redisConnectionConfiguration.redisConnectionFactory(builderCustomizers);
    }

    @ConditionalOnMissingBean(name = "commonRedisConnectionConfiguration")
    @Conditional(CommonRedisCondition.class)
    @Bean
    public JedisRedisConnectionConfiguration commonRedisConnectionConfiguration(Environment environment,
            ObjectProvider<RedisSentinelConfiguration> sentinelConfiguration,
            ObjectProvider<RedisClusterConfiguration> clusterConfiguration){
        RedisProperties properties = Binder.get(environment).bind("common.redis", RedisProperties.class).get();
        return new JedisRedisConnectionConfiguration(properties, sentinelConfiguration, clusterConfiguration);
    }

    @ConditionalOnMissingBean(name = "commonRedisConnectionFactory")
    @ConditionalOnBean(name = "commonRedisConnectionConfiguration")
    @Bean
    public JedisConnectionFactory commonRedisConnectionFactory(
            @Qualifier("commonRedisConnectionConfiguration") JedisRedisConnectionConfiguration redisConnectionConfiguration,
            ObjectProvider<JedisClientConfigurationBuilderCustomizer> builderCustomizers){
        return redisConnectionConfiguration.redisConnectionFactory(builderCustomizers);
    }
}
