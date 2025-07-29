/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;

import javax.validation.constraints.NotNull;

/**
 *
 * @author shanhuiming
 *
 */
@SuppressWarnings(value = { "unchecked", "rawtypes" })
public class RedisHelper{

    private RedisTemplate redisTemplate;

    public static RedisHelper newRedisHelper(RedisTemplate<Object, Object> template){
        return new RedisHelper(template);
    }

    public RedisHelper(RedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public RedisTemplate getRedisTemplate(){
        return redisTemplate;
    }

    public RedisSerializer getKeySerializer(){
        return redisTemplate.getKeySerializer();
    }

    public RedisSerializer getValueSerializer(){
        return redisTemplate.getValueSerializer();
    }

    /**
     * @see <a href="https://redis.io/commands/info">Redis Documentation: INFO</a>
     */
    public Properties info(){
        return (Properties) redisTemplate.execute((RedisCallback<Properties>) RedisServerCommands::info);
    }

    /**
     * @see <a href="https://redis.io/commands/ping">Redis Documentation: PING</a>
     */
    public void ping(){
        redisTemplate.execute((RedisCallback<String>) RedisConnectionCommands::ping);
    }

    /**
     * @see <a href="https://redis.io/commands/exists">Redis Documentation: EXISTS</a>
     */
    public boolean existKey(String key){
        return redisTemplate.hasKey(key);
    }

    /**
     * @see <a href="https://redis.io/commands/keys">Redis Documentation: KEYS</a>
     */
    public Collection<String> keys(String pattern){
        List<String> keys = new ArrayList<>();
        redisTemplate.execute((RedisConnection connection) -> {
            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build());
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
            return null;
        });
        return keys;
    }

    /**
     * @see <a href="https://redis.io/commands/del">Redis Documentation: DEL</a>
     */
    public Long delete(String... keys){
        if(ArrayUtils.isEmpty(keys)){
            return 0L;
        }
        return redisTemplate.delete(List.of(keys));
    }

    /**
     * @see <a href="https://redis.io/commands/del">Redis Documentation: DEL</a>
     */
    public Long delete(Collection<String> collection){
        return redisTemplate.delete(collection);
    }

    /**
     * @see <a href="https://redis.io/commands/pexpire">Redis Documentation: PEXPIRE</a>
     */
    public Boolean expire(String key, long timeout, TimeUnit unit){
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * @see <a href="https://redis.io/commands/ttl">Redis Documentation: TTL</a>
     */
    public Long getExpire(final String key){
        return redisTemplate.getExpire(key);
    }

    /**
     * @see <a href="https://redis.io/commands/ttl">Redis Documentation: TTL</a>
     */
    public Long getExpire(final String key, TimeUnit timeUnit){
        return redisTemplate.getExpire(key, timeUnit);
    }

    public <T> Map<String, T> pipeline(Map<String, java.util.function.Consumer<RedisOperations<String, Object>>> operationMap) {
        List<String> keys = new ArrayList<>(operationMap.keySet());
        List<T> results = redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(@NotNull RedisOperations redisOperations) {
                operationMap.forEach((key, consumer) -> consumer.accept(redisOperations));
                return null;
            }
        });
        return IntStream.range(0, keys.size()).boxed().collect(Collectors.toMap(keys::get, results::get));
    }

    public <T> List<T> pipeline(List<java.util.function.Consumer<RedisOperations<String, Object>>> operationList) {
        return redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(@NotNull RedisOperations redisOperations) {
                operationList.forEach(consumer -> consumer.accept(redisOperations));
                return null;
            }
        });
    }

    /* ******************************************
     * opsForValue
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/get">Redis Documentation: GET</a>
     */
    public <T> T getValue(String key){
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * @see <a href="https://redis.io/commands/getdel">Redis Documentation: GETDEL</a>
     */
    public <T> T getValueAndDelete(String key){
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.getAndDelete(key);
    }

    /**
     * @see <a href="https://redis.io/commands/getset">Redis Documentation: GETSET</a>
     */
    public <T> T getValueAndPut(String key, T value){
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.getAndSet(key, value);
    }

    /**
     * @see <a href="https://redis.io/commands/getex">Redis Documentation: GETEX</a>
     */
    public <T> T getValueAndExpire(String key, long timeout, TimeUnit timeUnit){
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.getAndExpire(key, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/getex">Redis Documentation: GETEX</a>
     */
    public <T> T getValueAndPersist(String key){
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.getAndPersist(key);
    }

    /**
     * @see <a href="https://redis.io/commands/mget">Redis Documentation: MGET</a>
     */
    public <T> List<T> getMultiValue(String... keys){
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.multiGet(List.of(keys));
    }

    /**
     * @see <a href="https://redis.io/commands/mget">Redis Documentation: MGET</a>
     */
    public <T> List<T> getMultiValue(Collection<String> keys){
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.multiGet(keys);
    }

    /**
     * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
     */
    public <T> void putValue(String key, T value){
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * @see <a href="https://redis.io/commands/setex">Redis Documentation: SETEX</a>
     */
    public <T> void putExpire(String key, T value, long timeout, TimeUnit timeUnit){
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/setnx">Redis Documentation: SETNX</a>
     */
    public <T> Boolean putValueIfAbsent(String key, T value){
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    /**
     * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
     */
    public <T> Boolean putExpireIfAbsent(String key, T value, long timeout, TimeUnit timeUnit){
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
     */
    public <T> Boolean putValueIfPresent(String key, T value){
        return redisTemplate.opsForValue().setIfPresent(key, value);
    }

    /**
     * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
     */
    public <T> Boolean putExpireIfPresent(String key, T value, long timeout, TimeUnit timeUnit){
        return redisTemplate.opsForValue().setIfPresent(key, value, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/mset">Redis Documentation: MSET</a>
     */
    public void putMultiValue(Map<String, Object> map){
        redisTemplate.opsForValue().multiSet(map);
    }

    /**
     * @see <a href="https://redis.io/commands/incrby">Redis Documentation: INCRBY</a>
     */
    public Long incrementValue(String key, int step){
        return redisTemplate.opsForValue().increment(key, step);
    }

    /**
     * @see <a href="https://redis.io/commands/decrby">Redis Documentation: DECRBY</a>
     */
    public Long decrementValue(String key, int step){
        return redisTemplate.opsForValue().decrement(key, step);
    }

    /* ******************************************
     * opsForHash
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/hlen">Redis Documentation: HLEN</a>
     */
    public Long sizeOfMap(String key) {
        return redisTemplate.opsForHash().size(key);
    }

    /**
     * @see <a href="https://redis.io/commands/hscan">Redis Documentation: HSCAN</a>
     */
    public <T> Cursor<Map.Entry<String, T>> scanMap(String key, ScanOptions scanOptions) {
        HashOperations<String, String, T> hashOps = redisTemplate.opsForHash();
        return hashOps.scan(key, scanOptions);
    }

    /**
     * @see <a href="https://redis.io/commands/hexits">Redis Documentation: HEXISTS</a>
     */
    public Boolean hasKeyInMap(String key, String hKey){
        return redisTemplate.opsForHash().hasKey(key, hKey);
    }

    /**
     * @see <a href="https://redis.io/commands/hgetall">Redis Documentation: HGETALL</a>
     */
    public <T> Map<String, T> getMap(String key){
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * @see <a href="https://redis.io/commands/hget">Redis Documentation: HGET</a>
     */
    public <T> T getMap(String key, String hKey){
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * @see <a href="https://redis.io/commands/hmget">Redis Documentation: HMGET</a>
     */
    public <T> List<T> getMultiMap(String key, Collection<String> hKeys){
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * @see <a href="https://redis.io/commands/hset">Redis Documentation: HSET</a>
     */
    public <T> void putMap(String key, String hKey, T value){
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * @see <a href="https://redis.io/commands/hmset">Redis Documentation: HMSET</a>
     */
    public void putMap(String key, Map<String, Object> map){
        if(map == null || map.isEmpty()) {
            return;
        }
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * @see <a href="https://redis.io/commands/hsetnx">Redis Documentation: HSETNX</a>
     */
    public <T> Boolean putMapIfAbsent(String key, String hKey, T value){
        HashOperations<String, String, T> hashOps = redisTemplate.opsForHash();
        return hashOps.putIfAbsent(key, hKey, value);
    }

    /**
     * @see <a href="https://redis.io/commands/hincrby">Redis Documentation: HINCRBY</a>
     */
    public Long incrementMap(String key, String hKey, long delta) {
        return redisTemplate.opsForHash().increment(key, hKey, delta);
    }

    /**
     * @see <a href="https://redis.io/commands/hdel">Redis Documentation: HDEL</a>
     */
    public Long removeFromMap(String key, String hKey){
        HashOperations hashOperations = redisTemplate.opsForHash();
        return hashOperations.delete(key, hKey);
    }

    /* ******************************************
     * opsForList
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/llen">Redis Documentation: LLEN</a>
     */
    public Long sizeOfList(String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * @see <a href="https://redis.io/commands/lpos">Redis Documentation: LPOS</a>
     */
    public <T> Long indexOfList(String key, T value){
        return redisTemplate.opsForList().indexOf(key, value);
    }

    /**
     * @see <a href="https://redis.io/commands/lpos">Redis Documentation: LPOS</a>
     */
    public <T> Long lastIndexOfList(String key, T value){
        return redisTemplate.opsForList().lastIndexOf(key, value);
    }

    /**
     * @see <a href="https://redis.io/commands/lindex">Redis Documentation: LINDEX</a>
     */
    public <T> T indexValueOfList(String key, long index){
        ListOperations<String, T> listOperations = redisTemplate.opsForList();
        return listOperations.index(key, index);
    }

    /**
     * @see <a href="https://redis.io/commands/lrange">Redis Documentation: LRANGE</a>
     */
    public <T> List<T> rangeOfList(String key, int start, int end){
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * @see <a href="https://redis.io/commands/lset">Redis Documentation: LSET</a>
     */
    public <T> void insertListByIndex(String key, long index, T value){
        redisTemplate.opsForList().set(key, index, value);
    }

    /**
     * @see <a href="https://redis.io/commands/linsert">Redis Documentation: LINSERT</a>
     */
    public <T> long insertListBefore(String key, T pivot, T value){
        Long count = redisTemplate.opsForList().leftPush(key, pivot, value);
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/linsert">Redis Documentation: LINSERT</a>
     */
    public <T> long insertListAfter(String key, T pivot, T value){
        Long count = redisTemplate.opsForList().rightPush(key, pivot, value);
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/lpush">Redis Documentation: LPUSH</a>
     */
    public <T> long pushListFromLeft(String key, T value){
        Long count = redisTemplate.opsForList().leftPush(key, value);
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/lpushx">Redis Documentation: LPUSHX</a>
     */
    public <T> long pushListFromLeftIfPresent(String key, T value){
        Long count = redisTemplate.opsForList().leftPushIfPresent(key, value);
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/lpush">Redis Documentation: LPUSH</a>
     */
    public long pushAllListFromLeft(String key, Object... values){
        Long count = redisTemplate.opsForList().leftPushAll(key, values);
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/lpush">Redis Documentation: LPUSH</a>
     */
    public long pushAllListFromLeft(String key, Collection<Object> values){
        Long count = redisTemplate.opsForList().leftPushAll(key, values);
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/rpush">Redis Documentation: RPUSH</a>
     */
    public <T> long pushListFromRight(String key, T value){
        Long count = redisTemplate.opsForList().rightPush(key, value);
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/rpushx">Redis Documentation: RPUSHX</a>
     */
    public <T> long pushListFromRightIfPresent(String key, T value){
        Long count = redisTemplate.opsForList().rightPushIfPresent(key, value);
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/rpush">Redis Documentation: RPUSH</a>
     */
    public long pushAllListFromRight(String key, Object... values){
        Long count = redisTemplate.opsForList().rightPushAll(key, values);
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/rpush">Redis Documentation: RPUSH</a>
     */
    public long pushAllListFromRight(String key, Collection<Object> values){
        Long count = redisTemplate.opsForList().rightPushAll(key, values);
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/lpop">Redis Documentation: LPOP</a>
     */
    public <T> T popListFromLeft(String key){
        ListOperations<String, T> listOperations = redisTemplate.opsForList();
        return listOperations.leftPop(key);
    }

    /**
     * @see <a href="https://redis.io/commands/blpop">Redis Documentation: BLPOP</a>
     */
    public <T> T popListFromLeft(String key, long timeout, TimeUnit timeUnit){
        ListOperations<String, T> listOperations = redisTemplate.opsForList();
        return listOperations.leftPop(key, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/rpop">Redis Documentation: RPOP</a>
     */
    public <T> T popListFromRight(String key){
        ListOperations<String, T> listOperations = redisTemplate.opsForList();
        return listOperations.rightPop(key);
    }

    /**
     * @see <a href="https://redis.io/commands/brpop">Redis Documentation: BRPOP</a>
     */
    public <T> T popListFromRight(String key, long timeout, TimeUnit timeUnit){
        ListOperations<String, T> listOperations = redisTemplate.opsForList();
        return listOperations.rightPop(key, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/rpoplpush">Redis Documentation: RPOPLPUSH</a>
     */
    public <T> T popListFromRightToLeft(String rightKey, String leftKey){
        ListOperations<String, T> listOperations = redisTemplate.opsForList();
        return listOperations.rightPopAndLeftPush(rightKey, leftKey);
    }

    /**
     * @see <a href="https://redis.io/commands/brpoplpush">Redis Documentation: BRPOPLPUSH</a>
     */
    public <T> T popListFromRightToLeft(String rightKey, String leftKey, long timeout, TimeUnit timeUnit){
        ListOperations<String, T> listOperations = redisTemplate.opsForList();
        return listOperations.rightPopAndLeftPush(rightKey, leftKey, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/lmove">Redis Documentation: LMOVE</a>
     */
    public <T> T moveList(String srcKey, RedisListCommands.Direction from, String destKey, RedisListCommands.Direction to){
        ListOperations<String, T> listOperations = redisTemplate.opsForList();
        return listOperations.move(srcKey, from, destKey, to);
    }

    /**
     * @see <a href="https://redis.io/commands/blmove">Redis Documentation: BLMOVE</a>
     */
    public <T> T moveList(String srcKey, RedisListCommands.Direction from, String destKey, RedisListCommands.Direction to, long timeout, TimeUnit timeUnit){
        ListOperations<String, T> listOperations = redisTemplate.opsForList();
        return listOperations.move(srcKey, from, destKey, to, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/lrem">Redis Documentation: LREM</a>
     */
    public <T> Long removeFromList(String key, T value, long count){
        return redisTemplate.opsForList().remove(key, count, value);
    }

    /* ******************************************
     * opsForSet
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/scard">Redis Documentation: SCARD</a>
     */
    public Long sizeOfSet(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * @see <a href="https://redis.io/commands/scan">Redis Documentation: SCAN</a>
     */
    public <T> Cursor<T> scanSet(String key, ScanOptions scanOptions) {
        return redisTemplate.opsForSet().scan(key, scanOptions);
    }

    /**
     * @see <a href="https://redis.io/commands/sismember">Redis Documentation: SISMEMBER</a>
     */
    public Boolean memberOfSet(String key, Object member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /**
     * @see <a href="https://redis.io/commands/smembers">Redis Documentation: SMEMBERS</a>
     */
    public <T> Set<T> getSet(String key){
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * @see <a href="https://redis.io/commands/sadd">Redis Documentation: SADD</a>
     */
    public void offerSet(String key, Set<Object> dataSet){
        BoundSetOperations<String, Object> setOperation = redisTemplate.boundSetOps(key);
        for (Object t : dataSet) {
            setOperation.add(t);
        }
    }

    /**
     * @see <a href="https://redis.io/commands/sadd">Redis Documentation: SADD</a>
     */
    public void offerSet(String key, Object... values){
        BoundSetOperations<String, Object> setOperation = redisTemplate.boundSetOps(key);
        for (Object t : values) {
            setOperation.add(t);
        }
    }

    /**
     * @see <a href="https://redis.io/commands/spop">Redis Documentation: SPOP</a>
     */
    public <T> List<T> popSet(String key, int count){
        return redisTemplate.opsForSet().pop(key, count);
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public <T> Set<T> intersectSet(Collection<String> keys){
        return redisTemplate.opsForSet().intersect(keys);
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public <T> Set<T> intersectSet(String key, Collection<String> others){
        return redisTemplate.opsForSet().intersect(key, others);
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public <T> Set<T> intersectSet(String key, String... others){
        return redisTemplate.opsForSet().intersect(key, List.of(others));
    }

    /**
     * @see <a href="https://redis.io/commands/sinterstore">Redis Documentation: SINTERSTORE</a>
     */
    public Long intersectSetAndStore(String destKey, Collection<String> keys){
        return redisTemplate.opsForSet().intersectAndStore(keys, destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/sinterstore">Redis Documentation: SINTERSTORE</a>
     */
    public Long intersectSetAndStore(String destKey, String... keys){
        return redisTemplate.opsForSet().intersectAndStore(List.of(keys), destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public <T> Set<T> unionSet(Collection<String> keys){
        return redisTemplate.opsForSet().union(keys);
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public <T> Set<T> unionSet(String key, Collection<String> others){
        return redisTemplate.opsForSet().union(key, others);
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public <T> Set<T> unionSet(String key, String... others){
        return redisTemplate.opsForSet().union(key, List.of(others));
    }

    /**
     * @see <a href="https://redis.io/commands/sunionstore">Redis Documentation: SUNIONSTORE</a>
     */
    public Long unionSetAndStore(String destKey, Collection<String> keys){
        return redisTemplate.opsForSet().unionAndStore(keys, destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/sunionstore">Redis Documentation: SUNIONSTORE</a>
     */
    public Long unionSetAndStore(String destKey, String... keys){
        return redisTemplate.opsForSet().unionAndStore(List.of(keys), destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/sdiff">Redis Documentation: SDIFF</a>
     */
    public <T> Set<T> diffSet(String key, String... others){
        return redisTemplate.opsForSet().difference(key, List.of(others));
    }

    /**
     * @see <a href="https://redis.io/commands/sdiff">Redis Documentation: SDIFF</a>
     */
    public <T> Set<T> diffSet(String key, Collection<String> others){
        return redisTemplate.opsForSet().difference(key, others);
    }

    /**
     * @see <a href="https://redis.io/commands/sdiffstore">Redis Documentation: SDIFFSTORE</a>
     */
    public Long diffSetAndStore(String destKey, String key, String... others){
        return redisTemplate.opsForSet().differenceAndStore(key, List.of(others), destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/sdiffstore">Redis Documentation: SDIFFSTORE</a>
     */
    public Long diffSetAndStore(String destKey, String key, Collection<String> others){
        return redisTemplate.opsForSet().differenceAndStore(key, others, destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/srem">Redis Documentation: SREM</a>
     */
    public Long removeFromSet(String key, Object... values){
        BoundSetOperations<String, Object> setOperation = redisTemplate.boundSetOps(key);
        return setOperation.remove(values);
    }

    /* ******************************************
     * opsForZSet
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/zcard">Redis Documentation: ZCARD</a>
     */
    public Long sizeOfZset(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    /**
     * @see <a href="https://redis.io/commands/zcount">Redis Documentation: ZCOUNT</a>
     */
    public Long countZsetByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    /**
     * @see <a href="https://redis.io/commands/zrank">Redis Documentation: ZRANK</a>
     */
    public <T> Long rankOfZset(String key, T value){
        return redisTemplate.opsForZSet().rank(key, value);
    }

    /**
     * @see <a href="https://redis.io/commands/zscore">Redis Documentation: ZSCORE</a>
     */
    public <T> Boolean memberOfZset(String key, T value) {
        return redisTemplate.opsForZSet().score(key, value) != null;
    }

    /**
     * @see <a href="https://redis.io/commands/zrange">Redis Documentation: ZRANGE</a>
     */
    public <T> T firstOfZset(String key){
        Set<T> set = redisTemplate.opsForZSet().range(key, 0, 0);
        if (set != null && !set.isEmpty()) {
            return set.iterator().next();
        }
        return null;
    }

    /**
     * @see <a href="https://redis.io/commands/zrange">Redis Documentation: ZRANGE</a>
     */
    public <T> Set<T> rangeOfZset(String key, long start, long end){
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis Documentation: ZRANGEBYSCORE</a>
     */
    public <T> Set<T> rangeOfZsetByScore(String key, double min, double max){
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    /**
     * @see <a href="https://redis.io/commands/zpopmin">Redis Documentation: ZPOPMIN</a>
     */
    public <T> Set<ZSetOperations.TypedTuple<T>> popMinOfZset(final String key, int count){
        return redisTemplate.opsForZSet().popMin(key, count);
    }

    /**
     * @see <a href="https://redis.io/commands/zpopmin">Redis Documentation: ZPOPMIN</a>
     */
    public <T> ZSetOperations.TypedTuple<T> popMinOfZset(String key){
        return redisTemplate.opsForZSet().popMin(key);
    }

    /**
     * @see <a href="https://redis.io/commands/bzpopmin">Redis Documentation: BZPOPMIN</a>
     */
    public <T> ZSetOperations.TypedTuple<T> popMinOfZset(String key, long timeout, TimeUnit timeUnit){
        return redisTemplate.opsForZSet().popMin(key, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/zpopmin">Redis Documentation: ZPOPMAX</a>
     */
    public <T> Set<ZSetOperations.TypedTuple<T>> popMaxOfZset(String key, int count){
        return redisTemplate.opsForZSet().popMax(key, count);
    }

    /**
     * @see <a href="https://redis.io/commands/zpopmin">Redis Documentation: ZPOPMAX</a>
     */
    public <T> ZSetOperations.TypedTuple<T> popMaxOfZset(String key){
        return redisTemplate.opsForZSet().popMax(key);
    }

    /**
     * @see <a href="https://redis.io/commands/bzpopmin">Redis Documentation: BZPOPMAX</a>
     */
    public <T> ZSetOperations.TypedTuple<T> popMaxOfZset(String key, long timeout, TimeUnit timeUnit){
        return redisTemplate.opsForZSet().popMax(key, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/zadd">Redis Documentation: ZADD</a>
     */
    public <T> void putZset(String key, T value, double score){
        redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * @see <a href="https://redis.io/commands/zadd">Redis Documentation: ZADD</a>
     */
    public <T> void putZset(final String key, Set<ZSetOperations.TypedTuple<T>> tuples){
        redisTemplate.opsForZSet().add(key, tuples);
    }

    /**
     * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
     */
    public Long removeFromZset(String key, Object... values){
        return redisTemplate.opsForZSet().remove(key, values);
    }

    /**
     * @see <a href="https://redis.io/commands/zremrangebyscore">Redis Documentation: ZREMRANGEBYSCORE</a>
     */
    public Long removeFromZsetByScore(String key, double min, double max){
        return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    /* ******************************************
     * opsForStream
     * ******************************************/

    public <K> StreamInfo.XInfoStream streamInfo(K key){
        return redisTemplate.opsForStream().info(key);
    }

    public String createStreamGroup(String key, String group) {
        return redisTemplate.opsForStream().createGroup(key, group);
    }

    /**
     * @see <a href="https://redis.io/commands/xadd">Redis Documentation: XADD</a>
     */
    public <T> RecordId publishStream(Record<String, T> record) {
        return redisTemplate.opsForStream().add(record);
    }

    /**
     * @see <a href="https://redis.io/commands/xread">Redis Documentation: XREAD</a>
     */
    public <K, HK, HV> List<MapRecord<K, HK, HV>> subscribeStream(Consumer consumer, StreamReadOptions readOptions, StreamOffset<K>... streams){
        return redisTemplate.opsForStream().read(consumer, readOptions, streams);
    }

    /**
     * @see <a href="https://redis.io/commands/xread">Redis Documentation: XREAD</a>
     */
    public <K, V> List<ObjectRecord<K, V>> subscribeStream(Class<V> targetType, Consumer consumer, StreamReadOptions readOptions, StreamOffset<K>... streams) {
        return redisTemplate.opsForStream().read(targetType, consumer, readOptions, streams);
    }

    /**
     * @see <a href="https://redis.io/commands/xack">Redis Documentation: XACK</a>
     */
    public Long ackStream(String key, String group, RecordId... recordIds){
        return redisTemplate.opsForStream().acknowledge(key, group, recordIds);
    }

    /**
     * @see <a href="https://redis.io/commands/xdel">Redis Documentation: XDEL</a>
     */
    public <K> Long deleteFromStream(K key, RecordId... recordIds){
        return redisTemplate.opsForStream().delete(key, recordIds);
    }

    /* ******************************************
     * channel
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/publish">Redis Documentation: PUBLISH</a>
     */
    public <T> void sendChannel(String channel, T message) {
        redisTemplate.convertAndSend(channel, message);
    }
}
