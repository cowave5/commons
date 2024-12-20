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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.api.redisnode.BaseRedisNodes;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.redisson.spring.starter.RedissonProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author shanhuiming
 *
 * 避免RedissonClient因为Redis连接失败而阻止进程启动
 */
@Slf4j
public class CheckedRedissonClient implements RedissonClient {

    private final ApplicationContext ctx;

    private final RedisProperties redisProperties;

    private final RedissonProperties redissonProperties;

    private final List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers;

    private RedissonClient redissonClient;

    public CheckedRedissonClient(ApplicationContext ctx, RedisProperties redisProperties, RedissonProperties redissonProperties,
                                 List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers, boolean exitOnConnectionFailed) throws IOException {
        this.ctx = ctx;
        this.redisProperties = redisProperties;
        this.redissonProperties = redissonProperties;
        this.redissonAutoConfigurationCustomizers = redissonAutoConfigurationCustomizers;
        try{
            redissonClient = redisson();
        } catch (Exception e) {
            if(!exitOnConnectionFailed){
                log.warn("Register a redissonClient that connect failed");
            }else{
                throw e;
            }
        }
    }

    public RedissonClient redisson() throws IOException {
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

        if (redissonProperties.getConfig() != null) {
            try {
                config = Config.fromYAML(redissonProperties.getConfig());
            } catch (IOException var13) {
                try {
                    config = Config.fromJSON(redissonProperties.getConfig());
                } catch (IOException var12) {
                    throw new IllegalArgumentException("Can't parse config", var12);
                }
            }
        } else if (redissonProperties.getFile() != null) {
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
        Iterator<String> it = nodesObject.iterator();
        while(it.hasNext()) {
            String node = it.next();
            if (!node.startsWith("redis://") && !node.startsWith("rediss://")) {
                nodes.add("redis://" + node);
            } else {
                nodes.add(node);
            }
        }
        return nodes.toArray(new String[nodes.size()]);
    }

    private InputStream getConfigStream(ApplicationContext ctx, RedissonProperties redissonProperties) throws IOException {
        Resource resource = ctx.getResource(redissonProperties.getFile());
        return resource.getInputStream();
    }

    /**
     * Returns RxJava Redisson instance
     *
     * @return redisson instance
     */
    @Override
    public RedissonRxClient rxJava() {
        return new CheckedRedissonRxClient(redissonClient);
    }

    /**
     * Returns Reactive Redisson instance
     *
     * @return redisson instance
     */
    @Override
    public RedissonReactiveClient reactive() {
        return new CheckedRedissonReactiveClient(redissonClient);
    }

    /**
     * Returns time-series instance by <code>name</code>
     *
     * @param name - name of instance
     * @return RTimeSeries object
     */
    @Override
    public <V> RTimeSeries<V> getTimeSeries(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getTimeSeries(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns time-series instance by <code>name</code>
     * using provided <code>codec</code> for values.
     *
     * @param name  - name of instance
     * @param codec - codec for values
     * @return RTimeSeries object
     */
    @Override
    public <V> RTimeSeries<V> getTimeSeries(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getTimeSeries(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns stream instance by <code>name</code>
     * <p>
     * Requires <b>Redis 5.0.0 and higher.</b>
     *
     * @param name of stream
     * @return RStream object
     */
    @Override
    public <K, V> RStream<K, V> getStream(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getStream(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns stream instance by <code>name</code>
     * using provided <code>codec</code> for entries.
     * <p>
     * Requires <b>Redis 5.0.0 and higher.</b>
     *
     * @param name  - name of stream
     * @param codec - codec for entry
     * @return RStream object
     */
    @Override
    public <K, V> RStream<K, V> getStream(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getStream(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns rate limiter instance by <code>name</code>
     *
     * @param name of rate limiter
     * @return RateLimiter object
     */
    @Override
    public RRateLimiter getRateLimiter(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getRateLimiter(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns binary stream holder instance by <code>name</code>
     *
     * @param name of binary stream
     * @return BinaryStream object
     */
    @Override
    public RBinaryStream getBinaryStream(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getBinaryStream(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns geospatial items holder instance by <code>name</code>.
     *
     * @param name - name of object
     * @return Geo object
     */
    @Override
    public <V> RGeo<V> getGeo(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getGeo(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns geospatial items holder instance by <code>name</code>
     * using provided codec for geospatial members.
     *
     * @param name  - name of object
     * @param codec - codec for value
     * @return Geo object
     */
    @Override
    public <V> RGeo<V> getGeo(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getGeo(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns set-based cache instance by <code>name</code>.
     * Supports value eviction with a given TTL value.
     *
     * <p>If eviction is not required then it's better to use regular map {@link #getSet(String, Codec)}.</p>
     *
     * @param name - name of object
     * @return SetCache object
     */
    @Override
    public <V> RSetCache<V> getSetCache(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getSetCache(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns set-based cache instance by <code>name</code>.
     * Supports value eviction with a given TTL value.
     *
     * <p>If eviction is not required then it's better to use regular map {@link #getSet(String, Codec)}.</p>
     *
     * @param name  - name of object
     * @param codec - codec for values
     * @return SetCache object
     */
    @Override
    public <V> RSetCache<V> getSetCache(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getSetCache(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns map-based cache instance by <code>name</code>
     * using provided <code>codec</code> for both cache keys and values.
     * Supports entry eviction with a given MaxIdleTime and TTL settings.
     * <p>
     * If eviction is not required then it's better to use regular map {@link #getMap(String, Codec)}.
     *
     * @param name  - object name
     * @param codec - codec for keys and values
     * @return MapCache object
     */
    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getMapCache(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns map-based cache instance by <code>name</code>
     * using provided <code>codec</code> for both cache keys and values.
     * Supports entry eviction with a given MaxIdleTime and TTL settings.
     * <p>
     * If eviction is not required then it's better to use regular map {@link #getMap(String, Codec)}.
     *
     * @param name    - object name
     * @param codec   - codec for keys and values
     * @param options - map options
     * @return MapCache object
     */
    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name, Codec codec, MapOptions<K, V> options) {
        return Optional.ofNullable(redissonClient).map(client -> client.getMapCache(name, codec, options))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns map-based cache instance by name.
     * Supports entry eviction with a given MaxIdleTime and TTL settings.
     * <p>
     * If eviction is not required then it's better to use regular map {@link #getMap(String)}.</p>
     *
     * @param name - name of object
     * @return MapCache object
     */
    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getMapCache(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns map-based cache instance by name.
     * Supports entry eviction with a given MaxIdleTime and TTL settings.
     * <p>
     * If eviction is not required then it's better to use regular map {@link #getMap(String)}.</p>
     *
     * @param name    - name of object
     * @param options - map options
     * @return MapCache object
     */
    @Override
    public <K, V> RMapCache<K, V> getMapCache(String name, MapOptions<K, V> options) {
        return Optional.ofNullable(redissonClient).map(client -> client.getMapCache(name, options))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns object holder instance by name.
     *
     * @param name - name of object
     * @return Bucket object
     */
    @Override
    public <V> RBucket<V> getBucket(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getBucket(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns object holder instance by name
     * using provided codec for object.
     *
     * @param name  - name of object
     * @param codec - codec for values
     * @return Bucket object
     */
    @Override
    public <V> RBucket<V> getBucket(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getBucket(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns interface for mass operations with Bucket objects.
     *
     * @return Buckets
     */
    @Override
    public RBuckets getBuckets() {
        return Optional.ofNullable(redissonClient).map(client -> client.getBuckets())
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns interface for mass operations with Bucket objects
     * using provided codec for object.
     *
     * @param codec - codec for bucket objects
     * @return Buckets
     */
    @Override
    public RBuckets getBuckets(Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.getBuckets(codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns HyperLogLog instance by name.
     *
     * @param name - name of object
     * @return HyperLogLog object
     */
    @Override
    public <V> RHyperLogLog<V> getHyperLogLog(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getHyperLogLog(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns HyperLogLog instance by name
     * using provided codec for hll objects.
     *
     * @param name  - name of object
     * @param codec - codec for values
     * @return HyperLogLog object
     */
    @Override
    public <V> RHyperLogLog<V> getHyperLogLog(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getHyperLogLog(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns list instance by name.
     *
     * @param name - name of object
     * @return List object
     */
    @Override
    public <V> RList<V> getList(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getList(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns list instance by name
     * using provided codec for list objects.
     *
     * @param name  - name of object
     * @param codec - codec for values
     * @return List object
     */
    @Override
    public <V> RList<V> getList(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getList(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns List based Multimap instance by name.
     *
     * @param name - name of object
     * @return ListMultimap object
     */
    @Override
    public <K, V> RListMultimap<K, V> getListMultimap(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getListMultimap(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns List based Multimap instance by name
     * using provided codec for both map keys and values.
     *
     * @param name  - name of object
     * @param codec - codec for keys and values
     * @return ListMultimap object
     */
    @Override
    public <K, V> RListMultimap<K, V> getListMultimap(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getListMultimap(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns List based Multimap instance by name.
     * Supports key-entry eviction with a given TTL value.
     *
     * <p>If eviction is not required then it's better to use regular map {@link #getSetMultimap(String)}.</p>
     *
     * @param name - name of object
     * @return ListMultimapCache object
     */
    @Override
    public <K, V> RListMultimapCache<K, V> getListMultimapCache(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getListMultimapCache(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns List based Multimap instance by name
     * using provided codec for both map keys and values.
     * Supports key-entry eviction with a given TTL value.
     *
     * <p>If eviction is not required then it's better to use regular map {@link #getSetMultimap(String, Codec)}.</p>
     *
     * @param name  - name of object
     * @param codec - codec for keys and values
     * @return ListMultimapCache object
     */
    @Override
    public <K, V> RListMultimapCache<K, V> getListMultimapCache(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getListMultimapCache(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns local cached map instance by name.
     * Configured by parameters of options-object.
     *
     * @param name    - name of object
     * @param options - local map options
     * @return LocalCachedMap object
     */
    @Override
    public <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String name, LocalCachedMapOptions<K, V> options) {
        return Optional.ofNullable(redissonClient).map(client -> client.getLocalCachedMap(name, options))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns local cached map instance by name
     * using provided codec. Configured by parameters of options-object.
     *
     * @param name    - name of object
     * @param codec   - codec for keys and values
     * @param options - local map options
     * @return LocalCachedMap object
     */
    @Override
    public <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String name, Codec codec, LocalCachedMapOptions<K, V> options) {
        return Optional.ofNullable(redissonClient).map(client -> client.getLocalCachedMap(name, codec, options))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns map instance by name.
     *
     * @param name - name of object
     * @return Map object
     */
    @Override
    public <K, V> RMap<K, V> getMap(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getMap(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns map instance by name.
     *
     * @param name    - name of object
     * @param options - map options
     * @return Map object
     */
    @Override
    public <K, V> RMap<K, V> getMap(String name, MapOptions<K, V> options) {
        return Optional.ofNullable(redissonClient).map(client -> client.getMap(name, options))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns map instance by name
     * using provided codec for both map keys and values.
     *
     * @param name  - name of object
     * @param codec - codec for keys and values
     * @return Map object
     */
    @Override
    public <K, V> RMap<K, V> getMap(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getMap(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns map instance by name
     * using provided codec for both map keys and values.
     *
     * @param name    - name of object
     * @param codec   - codec for keys and values
     * @param options - map options
     * @return Map object
     */
    @Override
    public <K, V> RMap<K, V> getMap(String name, Codec codec, MapOptions<K, V> options) {
        return Optional.ofNullable(redissonClient).map(client -> client.getMap(name, codec, options))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns Set based Multimap instance by name.
     *
     * @param name - name of object
     * @return SetMultimap object
     */
    @Override
    public <K, V> RSetMultimap<K, V> getSetMultimap(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getSetMultimap(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns Set based Multimap instance by name
     * using provided codec for both map keys and values.
     *
     * @param name  - name of object
     * @param codec - codec for keys and values
     * @return SetMultimap object
     */
    @Override
    public <K, V> RSetMultimap<K, V> getSetMultimap(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getSetMultimap(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns Set based Multimap instance by name.
     * Supports key-entry eviction with a given TTL value.
     *
     * <p>If eviction is not required then it's better to use regular map {@link #getSetMultimap(String)}.</p>
     *
     * @param name - name of object
     * @return SetMultimapCache object
     */
    @Override
    public <K, V> RSetMultimapCache<K, V> getSetMultimapCache(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getSetMultimapCache(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns Set based Multimap instance by name
     * using provided codec for both map keys and values.
     * Supports key-entry eviction with a given TTL value.
     *
     * <p>If eviction is not required then it's better to use regular map {@link #getSetMultimap(String, Codec)}.</p>
     *
     * @param name  - name of object
     * @param codec - codec for keys and values
     * @return SetMultimapCache object
     */
    @Override
    public <K, V> RSetMultimapCache<K, V> getSetMultimapCache(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<K, V>getSetMultimapCache(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns semaphore instance by name
     *
     * @param name - name of object
     * @return Semaphore object
     */
    @Override
    public RSemaphore getSemaphore(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getSemaphore(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns semaphore instance by name.
     * Supports lease time parameter for each acquired permit.
     *
     * @param name - name of object
     * @return PermitExpirableSemaphore object
     */
    @Override
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getPermitExpirableSemaphore(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns Lock instance by name.
     * <p>
     * Implements a <b>non-fair</b> locking so doesn't guarantees an acquire order by threads.
     * <p>
     * To increase reliability during failover, all operations wait for propagation to all Redis slaves.
     *
     * @param name - name of object
     * @return Lock object
     */
    @Override
    public RLock getLock(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getLock(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns Spin lock instance by name.
     * <p>
     * Implements a <b>non-fair</b> locking so doesn't guarantees an acquire order by threads.
     * <p>
     * Lock doesn't use a pub/sub mechanism
     *
     * @param name - name of object
     * @return Lock object
     */
    @Override
    public RLock getSpinLock(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getSpinLock(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns Spin lock instance by name with specified back off options.
     * <p>
     * Implements a <b>non-fair</b> locking so doesn't guarantees an acquire order by threads.
     * <p>
     * Lock doesn't use a pub/sub mechanism
     *
     * @param name    - name of object
     * @param backOff
     * @return Lock object
     */
    @Override
    public RLock getSpinLock(String name, LockOptions.BackOff backOff) {
        return Optional.ofNullable(redissonClient).map(client -> client.getSpinLock(name, backOff))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns MultiLock instance associated with specified <code>locks</code>
     *
     * @param locks - collection of locks
     * @return MultiLock object
     */
    @Override
    public RLock getMultiLock(RLock... locks) {
        return Optional.ofNullable(redissonClient).map(client -> client.getMultiLock(locks))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    @Override
    public RLock getRedLock(RLock... locks) {
        return Optional.ofNullable(redissonClient).map(client -> client.getRedLock(locks))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns Lock instance by name.
     * <p>
     * Implements a <b>fair</b> locking so it guarantees an acquire order by threads.
     * <p>
     * To increase reliability during failover, all operations wait for propagation to all Redis slaves.
     *
     * @param name - name of object
     * @return Lock object
     */
    @Override
    public RLock getFairLock(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getFairLock(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns ReadWriteLock instance by name.
     * <p>
     * To increase reliability during failover, all operations wait for propagation to all Redis slaves.
     *
     * @param name - name of object
     * @return Lock object
     */
    @Override
    public RReadWriteLock getReadWriteLock(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getReadWriteLock(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns set instance by name.
     *
     * @param name - name of object
     * @return Set object
     */
    @Override
    public <V> RSet<V> getSet(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getSet(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns set instance by name
     * using provided codec for set objects.
     *
     * @param name  - name of object
     * @param codec - codec for values
     * @return Set object
     */
    @Override
    public <V> RSet<V> getSet(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getSet(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns sorted set instance by name.
     * This sorted set uses comparator to sort objects.
     *
     * @param name - name of object
     * @return SortedSet object
     */
    @Override
    public <V> RSortedSet<V> getSortedSet(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getSortedSet(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns sorted set instance by name
     * using provided codec for sorted set objects.
     * This sorted set sorts objects using comparator.
     *
     * @param name  - name of object
     * @param codec - codec for values
     * @return SortedSet object
     */
    @Override
    public <V> RSortedSet<V> getSortedSet(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getSortedSet(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns Redis Sorted Set instance by name.
     * This sorted set sorts objects by object score.
     *
     * @param name - name of object
     * @return ScoredSortedSet object
     */
    @Override
    public <V> RScoredSortedSet<V> getScoredSortedSet(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getScoredSortedSet(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns Redis Sorted Set instance by name
     * using provided codec for sorted set objects.
     * This sorted set sorts objects by object score.
     *
     * @param name  - name of object
     * @param codec - codec for values
     * @return ScoredSortedSet object
     */
    @Override
    public <V> RScoredSortedSet<V> getScoredSortedSet(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getScoredSortedSet(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns String based Redis Sorted Set instance by name
     * All elements are inserted with the same score during addition,
     * in order to force lexicographical ordering
     *
     * @param name - name of object
     * @return LexSortedSet object
     */
    @Override
    public RLexSortedSet getLexSortedSet(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getLexSortedSet(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns topic instance by name.
     * <p>
     * Messages are delivered to all listeners attached to the same Redis setup.
     * <p>
     *
     * @param name - name of object
     * @return Topic object
     */
    @Override
    public RTopic getTopic(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getTopic(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns topic instance by name
     * using provided codec for messages.
     * <p>
     * Messages are delivered to all listeners attached to the same Redis setup.
     * <p>
     *
     * @param name  - name of object
     * @param codec - codec for message
     * @return Topic object
     */
    @Override
    public RTopic getTopic(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.getTopic(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns reliable topic instance by name.
     * <p>
     * Dedicated Redis connection is allocated per instance (subscriber) of this object.
     * Messages are delivered to all listeners attached to the same Redis setup.
     * <p>
     * Requires <b>Redis 5.0.0 and higher.</b>
     *
     * @param name - name of object
     * @return ReliableTopic object
     */
    @Override
    public RReliableTopic getReliableTopic(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getReliableTopic(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns reliable topic instance by name
     * using provided codec for messages.
     * <p>
     * Dedicated Redis connection is allocated per instance (subscriber) of this object.
     * Messages are delivered to all listeners attached to the same Redis setup.
     * <p>
     * Requires <b>Redis 5.0.0 and higher.</b>
     *
     * @param name  - name of object
     * @param codec - codec for message
     * @return ReliableTopic object
     */
    @Override
    public RReliableTopic getReliableTopic(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.getReliableTopic(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns topic instance satisfies by pattern name.
     * <p>
     * Supported glob-style patterns:
     * h?llo subscribes to hello, hallo and hxllo
     * h*llo subscribes to hllo and heeeello
     * h[ae]llo subscribes to hello and hallo, but not hillo
     *
     * @param pattern of the topic
     * @return PatterTopic object
     */
    @Override
    public RPatternTopic getPatternTopic(String pattern) {
        return Optional.ofNullable(redissonClient).map(client -> client.getPatternTopic(pattern))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns topic instance satisfies by pattern name
     * using provided codec for messages.
     * <p>
     * Supported glob-style patterns:
     * h?llo subscribes to hello, hallo and hxllo
     * h*llo subscribes to hllo and heeeello
     * h[ae]llo subscribes to hello and hallo, but not hillo
     *
     * @param pattern of the topic
     * @param codec   - codec for message
     * @return PatterTopic object
     */
    @Override
    public RPatternTopic getPatternTopic(String pattern, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.getPatternTopic(pattern, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded queue instance by name.
     *
     * @param name of object
     * @return queue object
     */
    @Override
    public <V> RQueue<V> getQueue(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getQueue(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns transfer queue instance by name.
     *
     * @param name - name of object
     * @return TransferQueue object
     */
    @Override
    public <V> RTransferQueue<V> getTransferQueue(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getTransferQueue(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns transfer queue instance by name
     * using provided codec for queue objects.
     *
     * @param name  - name of object
     * @param codec - code for values
     * @return TransferQueue object
     */
    @Override
    public <V> RTransferQueue<V> getTransferQueue(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getTransferQueue(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded delayed queue instance by name.
     * <p>
     * Could be attached to destination queue only.
     * All elements are inserted with transfer delay to destination queue.
     *
     * @param destinationQueue - destination queue
     * @return Delayed queue object
     */
    @Override
    public <V> RDelayedQueue<V> getDelayedQueue(RQueue<V> destinationQueue) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getDelayedQueue(destinationQueue))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded queue instance by name
     * using provided codec for queue objects.
     *
     * @param name  - name of object
     * @param codec - codec for message
     * @return Queue object
     */
    @Override
    public <V> RQueue<V> getQueue(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getQueue(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns RingBuffer based queue.
     *
     * @param name - name of object
     * @return RingBuffer object
     */
    @Override
    public <V> RRingBuffer<V> getRingBuffer(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getRingBuffer(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns RingBuffer based queue.
     *
     * @param name  - name of object
     * @param codec - codec for values
     * @return RingBuffer object
     */
    @Override
    public <V> RRingBuffer<V> getRingBuffer(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getRingBuffer(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns priority unbounded queue instance by name.
     * It uses comparator to sort objects.
     *
     * @param name of object
     * @return Queue object
     */
    @Override
    public <V> RPriorityQueue<V> getPriorityQueue(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getPriorityQueue(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns priority unbounded queue instance by name
     * using provided codec for queue objects.
     * It uses comparator to sort objects.
     *
     * @param name  - name of object
     * @param codec - codec for message
     * @return Queue object
     */
    @Override
    public <V> RPriorityQueue<V> getPriorityQueue(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getPriorityQueue(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded priority blocking queue instance by name.
     * It uses comparator to sort objects.
     *
     * @param name of object
     * @return Queue object
     */
    @Override
    public <V> RPriorityBlockingQueue<V> getPriorityBlockingQueue(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getPriorityBlockingQueue(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded priority blocking queue instance by name
     * using provided codec for queue objects.
     * It uses comparator to sort objects.
     *
     * @param name  - name of object
     * @param codec - codec for message
     * @return Queue object
     */
    @Override
    public <V> RPriorityBlockingQueue<V> getPriorityBlockingQueue(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getPriorityBlockingQueue(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded priority blocking deque instance by name.
     * It uses comparator to sort objects.
     *
     * @param name of object
     * @return Queue object
     */
    @Override
    public <V> RPriorityBlockingDeque<V> getPriorityBlockingDeque(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getPriorityBlockingDeque(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded priority blocking deque instance by name
     * using provided codec for queue objects.
     * It uses comparator to sort objects.
     *
     * @param name  - name of object
     * @param codec - codec for message
     * @return Queue object
     */
    @Override
    public <V> RPriorityBlockingDeque<V> getPriorityBlockingDeque(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getPriorityBlockingDeque(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns priority unbounded deque instance by name.
     * It uses comparator to sort objects.
     *
     * @param name of object
     * @return Queue object
     */
    @Override
    public <V> RPriorityDeque<V> getPriorityDeque(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getPriorityDeque(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns priority unbounded deque instance by name
     * using provided codec for queue objects.
     * It uses comparator to sort objects.
     *
     * @param name  - name of object
     * @param codec - codec for message
     * @return Queue object
     */
    @Override
    public <V> RPriorityDeque<V> getPriorityDeque(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getPriorityDeque(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded blocking queue instance by name.
     *
     * @param name - name of object
     * @return BlockingQueue object
     */
    @Override
    public <V> RBlockingQueue<V> getBlockingQueue(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getBlockingQueue(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded blocking queue instance by name
     * using provided codec for queue objects.
     *
     * @param name  - name of queue
     * @param codec - queue objects codec
     * @return BlockingQueue object
     */
    @Override
    public <V> RBlockingQueue<V> getBlockingQueue(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getBlockingQueue(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns bounded blocking queue instance by name.
     *
     * @param name of queue
     * @return BoundedBlockingQueue object
     */
    @Override
    public <V> RBoundedBlockingQueue<V> getBoundedBlockingQueue(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getBoundedBlockingQueue(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns bounded blocking queue instance by name
     * using provided codec for queue objects.
     *
     * @param name  - name of queue
     * @param codec - codec for values
     * @return BoundedBlockingQueue object
     */
    @Override
    public <V> RBoundedBlockingQueue<V> getBoundedBlockingQueue(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getBoundedBlockingQueue(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded deque instance by name.
     *
     * @param name - name of object
     * @return Deque object
     */
    @Override
    public <V> RDeque<V> getDeque(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getDeque(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded deque instance by name
     * using provided codec for deque objects.
     *
     * @param name  - name of object
     * @param codec - codec for values
     * @return Deque object
     */
    @Override
    public <V> RDeque<V> getDeque(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getDeque(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded blocking deque instance by name.
     *
     * @param name - name of object
     * @return BlockingDeque object
     */
    @Override
    public <V> RBlockingDeque<V> getBlockingDeque(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getBlockingDeque(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns unbounded blocking deque instance by name
     * using provided codec for deque objects.
     *
     * @param name  - name of object
     * @param codec - deque objects codec
     * @return BlockingDeque object
     */
    @Override
    public <V> RBlockingDeque<V> getBlockingDeque(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getBlockingDeque(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns atomicLong instance by name.
     *
     * @param name - name of object
     * @return AtomicLong object
     */
    @Override
    public RAtomicLong getAtomicLong(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getAtomicLong(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns atomicDouble instance by name.
     *
     * @param name - name of object
     * @return AtomicDouble object
     */
    @Override
    public RAtomicDouble getAtomicDouble(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getAtomicDouble(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns LongAdder instances by name.
     *
     * @param name - name of object
     * @return LongAdder object
     */
    @Override
    public RLongAdder getLongAdder(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getLongAdder(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns DoubleAdder instances by name.
     *
     * @param name - name of object
     * @return LongAdder object
     */
    @Override
    public RDoubleAdder getDoubleAdder(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getDoubleAdder(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns countDownLatch instance by name.
     *
     * @param name - name of object
     * @return CountDownLatch object
     */
    @Override
    public RCountDownLatch getCountDownLatch(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getCountDownLatch(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns bitSet instance by name.
     *
     * @param name - name of object
     * @return BitSet object
     */
    @Override
    public RBitSet getBitSet(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getBitSet(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns bloom filter instance by name.
     *
     * @param name - name of object
     * @return BloomFilter object
     */
    @Override
    public <V> RBloomFilter<V> getBloomFilter(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getBloomFilter(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns bloom filter instance by name
     * using provided codec for objects.
     *
     * @param name  - name of object
     * @param codec - codec for values
     * @return BloomFilter object
     */
    @Override
    public <V> RBloomFilter<V> getBloomFilter(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.<V>getBloomFilter(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns id generator by name.
     *
     * @param name - name of object
     * @return IdGenerator object
     */
    @Override
    public RIdGenerator getIdGenerator(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getIdGenerator(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns script operations object
     *
     * @return Script object
     */
    @Override
    public RScript getScript() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::getScript)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns script operations object using provided codec.
     *
     * @param codec - codec for params and result
     * @return Script object
     */
    @Override
    public RScript getScript(Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.getScript(codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns ScheduledExecutorService by name
     *
     * @param name - name of object
     * @return ScheduledExecutorService object
     */
    @Override
    public RScheduledExecutorService getExecutorService(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getExecutorService(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns ScheduledExecutorService by name
     *
     * @param name    - name of object
     * @param options - options for executor
     * @return ScheduledExecutorService object
     */
    @Override
    public RScheduledExecutorService getExecutorService(String name, ExecutorOptions options) {
        return Optional.ofNullable(redissonClient).map(client -> client.getExecutorService(name, options))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns ScheduledExecutorService by name
     * using provided codec for task, response and request serialization
     *
     * @param name  - name of object
     * @param codec - codec for task, response and request
     * @return ScheduledExecutorService object
     * @since 2.8.2
     */
    @Override
    public RScheduledExecutorService getExecutorService(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.getExecutorService(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns ScheduledExecutorService by name
     * using provided codec for task, response and request serialization
     *
     * @param name    - name of object
     * @param codec   - codec for task, response and request
     * @param options - options for executor
     * @return ScheduledExecutorService object
     */
    @Override
    public RScheduledExecutorService getExecutorService(String name, Codec codec, ExecutorOptions options) {
        return Optional.ofNullable(redissonClient).map(client -> client.getExecutorService(name, codec, options))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns object for remote operations prefixed with the default name (redisson_remote_service)
     *
     * @return RemoteService object
     */
    @Override
    public RRemoteService getRemoteService() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::getRemoteService)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns object for remote operations prefixed with the default name (redisson_remote_service)
     * and uses provided codec for method arguments and result.
     *
     * @param codec - codec for response and request
     * @return RemoteService object
     */
    @Override
    public RRemoteService getRemoteService(Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.getRemoteService(codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns object for remote operations prefixed with the specified name
     *
     * @param name - the name used as the Redis key prefix for the services
     * @return RemoteService object
     */
    @Override
    public RRemoteService getRemoteService(String name) {
        return Optional.ofNullable(redissonClient).map(client -> client.getRemoteService(name))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns object for remote operations prefixed with the specified name
     * and uses provided codec for method arguments and result.
     *
     * @param name  - the name used as the Redis key prefix for the services
     * @param codec - codec for response and request
     * @return RemoteService object
     */
    @Override
    public RRemoteService getRemoteService(String name, Codec codec) {
        return Optional.ofNullable(redissonClient).map(client -> client.getRemoteService(name, codec))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Creates transaction with <b>READ_COMMITTED</b> isolation level.
     *
     * @param options - transaction configuration
     * @return Transaction object
     */
    @Override
    public RTransaction createTransaction(TransactionOptions options) {
        return Optional.ofNullable(redissonClient).map(client -> client.createTransaction(options))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Creates batch object which could be executed later
     * with collected group of commands in pipeline mode.
     * <p>
     * See <a href="http://redis.io/topics/pipelining">http://redis.io/topics/pipelining</a>
     *
     * @param options - batch configuration
     * @return Batch object
     */
    @Override
    public RBatch createBatch(BatchOptions options) {
        return Optional.ofNullable(redissonClient).map(client -> client.createBatch(options))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Creates batch object which could be executed later
     * with collected group of commands in pipeline mode.
     * <p>
     * See <a href="http://redis.io/topics/pipelining">http://redis.io/topics/pipelining</a>
     *
     * @return Batch object
     */
    @Override
    public RBatch createBatch() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::createBatch)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns interface with methods for Redis keys.
     * Each of Redis/Redisson object associated with own key
     *
     * @return Keys object
     */
    @Override
    public RKeys getKeys() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::getKeys)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns RedissonAttachedLiveObjectService which can be used to
     * retrieve live REntity(s)
     *
     * @return LiveObjectService object
     */
    @Override
    public RLiveObjectService getLiveObjectService() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::getLiveObjectService)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Shutdown Redisson instance but <b>NOT</b> Redis server
     * <p>
     * This equates to invoke shutdown(0, 2, TimeUnit.SECONDS);
     */
    @Override
    public void shutdown() {
        Optional.ofNullable(redissonClient).ifPresentOrElse(client -> client.shutdown(),
                () -> { throw new UnsupportedOperationException("redisson not connected"); });
    }

    /**
     * Shuts down Redisson instance but <b>NOT</b> Redis server
     * <p>
     * Shutdown ensures that no tasks are submitted for <i>'the quiet period'</i>
     * (usually a couple seconds) before it shuts itself down.  If a task is submitted during the quiet period,
     * it is guaranteed to be accepted and the quiet period will start over.
     *
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout     the maximum amount of time to wait until the executor is {@linkplain #shutdown()}
     *                    regardless if a task was submitted during the quiet period
     * @param unit        the unit of {@code quietPeriod} and {@code timeout}
     */
    @Override
    public void shutdown(long quietPeriod, long timeout, TimeUnit unit) {
        Optional.ofNullable(redissonClient).ifPresentOrElse(client -> client.shutdown(quietPeriod, timeout, unit),
                () -> { throw new UnsupportedOperationException("redisson not connected"); });
    }

    /**
     * Allows to get configuration provided
     * during Redisson instance creation. Further changes on
     * this object not affect Redisson instance.
     *
     * @return Config object
     */
    @Override
    public Config getConfig() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::getConfig)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns API to manage Redis nodes
     *
     * @param nodes Redis nodes API class
     * @return Redis nodes API object
     * @see RedisNodes#CLUSTER
     * @see RedisNodes#MASTER_SLAVE
     * @see RedisNodes#SENTINEL_MASTER_SLAVE
     * @see RedisNodes#SINGLE
     */
    @Override
    public <T extends BaseRedisNodes> T getRedisNodes(RedisNodes<T> nodes) {
        return Optional.ofNullable(redissonClient).map(client -> client.getRedisNodes(nodes))
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    @Override
    public NodesGroup<Node> getNodesGroup() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::getNodesGroup)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    @Override
    public ClusterNodesGroup getClusterNodesGroup() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::getClusterNodesGroup)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns {@code true} if this Redisson instance has been shut down.
     *
     * @return {@code true} if this Redisson instance has been shut down overwise <code>false</code>
     */
    @Override
    public boolean isShutdown() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::isShutdown)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns {@code true} if this Redisson instance was started to be shutdown
     * or was shutdown {@link #isShutdown()} already.
     *
     * @return {@code true} if this Redisson instance was started to be shutdown
     * or was shutdown {@link #isShutdown()} already.
     */
    @Override
    public boolean isShuttingDown() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::isShuttingDown)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }

    /**
     * Returns id of this Redisson instance
     *
     * @return id
     */
    @Override
    public String getId() {
        return Optional.ofNullable(redissonClient).map(RedissonClient::getId)
            .orElseThrow(() -> new UnsupportedOperationException("redisson not connected"));
    }
}
