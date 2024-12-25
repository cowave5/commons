/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 *
 * @author shanhuiming
 *
 */
@AutoConfigureBefore(org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class)
@ConditionalOnClass(RedisOperations.class)
public class RedisAutoConfiguration {

    @ConditionalOnMissingBean(RedisTemplate.class)
    @Primary
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        // key-value的序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // Hash中key-value的序列话
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @ConditionalOnMissingBean(RedisHelper.class)
    @Primary
    @Bean
    public RedisHelper redisHelper(RedisTemplate<Object, Object> redisTemplate){
        return RedisHelper.newRedisHelper(redisTemplate);
    }

    @ConditionalOnMissingBean(StringRedisTemplate.class)
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @ConditionalOnMissingBean(StringRedisHelper.class)
    @Primary
    @Bean
    public StringRedisHelper stringRedisHelper(StringRedisTemplate stringRedisTemplate,
            @Value("${spring.redis.exitOnConnectionFailed:false}") boolean exitOnConnectionFailed){
        if(exitOnConnectionFailed && !"PONG".equals(stringRedisTemplate.execute(RedisConnectionCommands::ping))){
            throw new IllegalStateException("Redis connection failed");
        }
        return StringRedisHelper.newStringRedisHelper(stringRedisTemplate);
    }

    @ConditionalOnBean(name = "commonRedisConnectionFactory")
    @Bean
    public RedisTemplate<Object, Object> commonRedisTemplate(
            @Qualifier("commonRedisConnectionFactory") RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        // key-value的序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // Hash中key-value的序列话
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @ConditionalOnBean(name = "commonRedisTemplate")
    @Bean
    public RedisHelper commonRedisHelper(
            @Qualifier("commonRedisTemplate") RedisTemplate<Object, Object> redisTemplate){
        return RedisHelper.newRedisHelper(redisTemplate);
    }

    @ConditionalOnBean(name = "commonRedisConnectionFactory")
    @Bean
    public StringRedisTemplate commonStringRedisTemplate(
            @Qualifier("commonRedisConnectionFactory") RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @ConditionalOnBean(name = "commonStringRedisTemplate")
    @Bean
    public StringRedisHelper commonStringRedisHelper(
            @Qualifier("commonStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
            @Value("${common.redis.exitOnConnectionFailed:false}") boolean exitOnConnectionFailed){
        if(exitOnConnectionFailed && !"PONG".equals(stringRedisTemplate.execute(RedisConnectionCommands::ping))){
            throw new IllegalStateException("Redis connection failed");
        }
        return StringRedisHelper.newStringRedisHelper(stringRedisTemplate);
    }
}
