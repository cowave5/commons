/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
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
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonRxClient;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.redisson.spring.starter.RedissonProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
                                         @Nullable List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers) {
        return getRedissonClient(ctx, redisProperties, redissonProperties, redissonAutoConfigurationCustomizers);
    }

    public RedissonClient getRedissonClient(ApplicationContext ctx, RedisProperties redisProperties, RedissonProperties redissonProperties,
                                         @Nullable List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers) {
        Config config;
        Method clusterMethod = ReflectionUtils.findMethod(RedisProperties.class, "getCluster");
        Method timeoutMethod = ReflectionUtils.findMethod(RedisProperties.class, "getTimeout");
        Object timeoutValue = ReflectionUtils.invokeMethod(timeoutMethod, redisProperties);
        int timeout;
        Method nodesMethod;
        if (null == timeoutValue) {
            timeout = 10000;
        } else if (!(timeoutValue instanceof Integer)) {
            nodesMethod = ReflectionUtils.findMethod(timeoutValue.getClass(), "toMillis");
            timeout = ((Long)ReflectionUtils.invokeMethod(nodesMethod, timeoutValue)).intValue();
        } else {
            timeout = (Integer)timeoutValue;
        }

        if (redissonProperties != null && redissonProperties.getConfig() != null) {
            try {
                config = Config.fromYAML(redissonProperties.getConfig());
            } catch (IOException var13) {
                try {
                    config = Config.fromJSON(redissonProperties.getConfig());
                } catch (IOException var12) {
                    throw new IllegalArgumentException("Can't parse config", var12);
                }
            }
        } else if (redissonProperties != null && redissonProperties.getFile() != null) {
            try {
                InputStream is = this.getConfigStream(ctx, redissonProperties);
                config = Config.fromYAML(is);
            } catch (IOException var11) {
                try {
                    InputStream is = this.getConfigStream(ctx, redissonProperties);
                    config = Config.fromJSON(is);
                } catch (IOException var10) {
                    throw new IllegalArgumentException("Can't parse config", var10);
                }
            }
        } else if (redisProperties.getSentinel() != null) {
            nodesMethod = ReflectionUtils.findMethod(RedisProperties.Sentinel.class, "getNodes");
            Object nodesValue = ReflectionUtils.invokeMethod(nodesMethod, redisProperties.getSentinel());
            String[] nodes;
            if (nodesValue instanceof String) {
                nodes = this.convert(Arrays.asList(((String)nodesValue).split(",")));
            } else {
                nodes = this.convert((List)nodesValue);
            }
            config = new Config();
            SentinelServersConfig sentinelServersConfig = config.useSentinelServers()
                    .setMasterName(redisProperties.getSentinel().getMaster())
                    .addSentinelAddress(nodes)
                    .setDatabase(redisProperties.getDatabase())
                    .setConnectTimeout(timeout);
            // Sentinel判断下有效密码才设置
            if(StringUtils.isNotBlank(redisProperties.getPassword())){
                sentinelServersConfig.setPassword(redisProperties.getPassword());
            }
        } else {
            Method method;
            if (clusterMethod != null && ReflectionUtils.invokeMethod(clusterMethod, redisProperties) != null) {
                Object clusterObject = ReflectionUtils.invokeMethod(clusterMethod, redisProperties);
                method = ReflectionUtils.findMethod(clusterObject.getClass(), "getNodes");
                List<String> nodesObject = (List)ReflectionUtils.invokeMethod(method, clusterObject);
                String[] nodes = this.convert(nodesObject);
                config = new Config();
                config.useClusterServers().addNodeAddress(nodes).setConnectTimeout(timeout).setPassword(redisProperties.getPassword());
            } else {
                config = new Config();
                String prefix = "redis://";
                method = ReflectionUtils.findMethod(RedisProperties.class, "isSsl");
                if (method != null && (Boolean)ReflectionUtils.invokeMethod(method, redisProperties)) {
                    prefix = "rediss://";
                }
                config.useSingleServer().setAddress(prefix + redisProperties.getHost() + ":" + redisProperties.getPort()).setConnectTimeout(timeout).setDatabase(redisProperties.getDatabase()).setPassword(redisProperties.getPassword());
            }
        }

        if (redissonAutoConfigurationCustomizers != null) {
            for (RedissonAutoConfigurationCustomizer customizer : redissonAutoConfigurationCustomizers) {
                customizer.customize(config);
            }
        }
        return Redisson.create(config);
    }

    private String[] convert(List<String> nodesObject) {
        List<String> nodes = new ArrayList<>(nodesObject.size());
        for (String node : nodesObject) {
            if (!node.startsWith("redis://") && !node.startsWith("rediss://")) {
                nodes.add("redis://" + node);
            } else {
                nodes.add(node);
            }
        }
        return nodes.toArray(new String[0]);
    }

    private InputStream getConfigStream(ApplicationContext ctx, RedissonProperties redissonProperties) throws IOException {
        Resource resource = ctx.getResource(redissonProperties.getFile());
        return resource.getInputStream();
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

    @ConditionalOnProperty(name = "common.redis.redisson", havingValue = "true", matchIfMissing = true)
    @Conditional(CommonRedisCondition.class)
    @Bean(destroyMethod = "shutdown")
    public RedissonClient commonRedissonClient(ApplicationContext ctx, Environment environment,
                                               @Nullable List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers) {
        RedisProperties redisProperties = Binder.get(environment).bind("common.redis", RedisProperties.class).get();
        RedissonProperties redissonProperties = Binder.get(environment).bind("common.redis.redisson", RedissonProperties.class).orElse(null);
        return getRedissonClient(ctx, redisProperties, redissonProperties, redissonAutoConfigurationCustomizers);
    }

    @ConditionalOnProperty(name = "common.redis.redisson", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(name = "commonRedissonClient")
    @Lazy
    @Bean
    public RedissonReactiveClient publicRedissonReactive(@Qualifier("commonRedissonClient") RedissonClient redissonClient) {
        return redissonClient.reactive();
    }

    @ConditionalOnProperty(name = "common.redis.redisson", havingValue = "true", matchIfMissing = true)
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
