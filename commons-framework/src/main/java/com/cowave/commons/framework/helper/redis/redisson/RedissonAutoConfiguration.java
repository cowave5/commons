/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis.redisson;

import com.cowave.commons.framework.configuration.ApplicationProperties;
import com.cowave.commons.framework.helper.redis.connection.CommonRedisCondition;
import com.cowave.commons.framework.helper.redis.connection.JedisAutoConfiguration;
import com.cowave.commons.framework.helper.redis.connection.LettuceAutoConfiguration;
import com.cowave.commons.framework.helper.redis.redisson.lock.RedissonLockAspect;
import com.cowave.commons.framework.helper.redis.redisson.lock.RedissonLockHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonRxClient;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.redisson.spring.starter.RedissonProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisOperations;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
@AutoConfigureBefore({org.redisson.spring.starter.RedissonAutoConfiguration.class, LettuceAutoConfiguration.class, JedisAutoConfiguration.class})
@ConditionalOnClass({Redisson.class, RedisOperations.class})
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties({RedisProperties.class, RedissonProperties.class})
public class RedissonAutoConfiguration {

    @ConditionalOnMissingBean(RedissonClient.class)
    @Primary
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(ApplicationContext ctx, RedisProperties redisProperties, RedissonProperties redissonProperties,
                                         @Nullable List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers,
                                         @Value("${spring.redis.exitOnConnectionFailed:false}") boolean exitOnConnectionFailed) throws IOException {
        return new CheckedRedissonClient(ctx, redisProperties, redissonProperties, redissonAutoConfigurationCustomizers, exitOnConnectionFailed);
    }

    @ConditionalOnMissingBean(RedissonReactiveClient.class)
    @Primary
    @Lazy
    @Bean
    public RedissonReactiveClient redissonReactiveClient(RedissonClient redissonClient) {
        return redissonClient.reactive();
    }

    @ConditionalOnMissingBean(RedissonRxClient.class)
    @Primary
    @Lazy
    @Bean
    public RedissonRxClient redissonRxClient(RedissonClient redissonClient) {
        return redissonClient.rxJava();
    }

    @Conditional(CommonRedisCondition.class)
    @Bean(destroyMethod = "shutdown")
    public RedissonClient commonRedissonClient(ApplicationContext ctx, Environment environment,
                                               @Nullable List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers,
                                               @Value("${common.redis.exitOnConnectionFailed:false}") boolean exitOnConnectionFailed) throws IOException {
        RedisProperties redisProperties = Binder.get(environment).bind("common.redis", RedisProperties.class).get();
        RedissonProperties redissonProperties = Binder.get(environment).bind("common.redis.redisson", RedissonProperties.class).orElse(null);
        return new CheckedRedissonClient(ctx, redisProperties, redissonProperties, redissonAutoConfigurationCustomizers, exitOnConnectionFailed);
    }

    @ConditionalOnBean(name = "commonRedissonClient")
    @Lazy
    @Bean
    public RedissonReactiveClient publicRedissonReactive(@Qualifier("commonRedissonClient") RedissonClient redissonClient) {
        return redissonClient.reactive();
    }

    @ConditionalOnBean(name = "commonRedissonClient")
    @Lazy
    @Bean
    public RedissonRxClient publicredissonRxJava(@Qualifier("commonRedissonClient") RedissonClient redissonClient) {
        return redissonClient.rxJava();
    }

    @ConditionalOnBean(RedissonClient.class)
    @Bean
    public RedissonLockHelper redissonLockHelper(
            RedissonClient redissonClient, ApplicationProperties applicationProperties){
        return new RedissonLockHelper(redissonClient, applicationProperties);
    }

    @ConditionalOnBean(RedissonLockHelper.class)
    @Bean
    public RedissonLockAspect redissonLockAspect(RedissonLockHelper redissonLockHelper){
        return new RedissonLockAspect(redissonLockHelper);
    }
}
