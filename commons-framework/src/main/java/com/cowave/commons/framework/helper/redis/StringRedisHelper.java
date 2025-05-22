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

import com.cowave.commons.client.http.asserts.Asserts;
import com.cowave.commons.tools.Collections;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author shanhuiming
 *
 */
public class StringRedisHelper {

    private static ObjectMapper MAPPER = new ObjectMapper();

    static{
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String LUA_CLEAN = """
            local cursor = "0"
            repeat
                local result = redis.call("SCAN", cursor, "MATCH", ARGV[1], "COUNT", 100)
                cursor = result[1]
                for _, key in ipairs(result[2]) do
                    redis.call("DEL", key)
                end
            until cursor == "0"
            """;

    private StringRedisTemplate stringRedisTemplate;

    public static StringRedisHelper newStringRedisHelper(StringRedisTemplate stringRedisTemplate){
        return new StringRedisHelper(stringRedisTemplate);
    }

    public StringRedisHelper(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public StringRedisTemplate getRedisTemplate() {
        return stringRedisTemplate;
    }

    public RedisSerializer<String> getStringSerializer(){
        return stringRedisTemplate.getStringSerializer();
    }

    /**
     * @see <a href="https://redis.io/commands/info">Redis Documentation: INFO</a>
     */
    public Properties info(){
        return stringRedisTemplate.execute((RedisCallback<Properties>) RedisServerCommands::info);
    }

    /**
     * @see <a href="https://redis.io/commands/ping">Redis Documentation: PING</a>
     */
    public String ping(){
        return stringRedisTemplate.execute(RedisConnectionCommands::ping);
    }

    /**
     * @see <a href="https://redis.io/commands/exists">Redis Documentation: EXISTS</a>
     */
    public boolean existKey(String key){
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * @see <a href="https://redis.io/commands/keys">Redis Documentation: KEYS</a>
     */
    public Collection<String> keys(String pattern){
        List<String> keys = new ArrayList<>();
        stringRedisTemplate.execute((RedisConnection connection) -> {
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
        return stringRedisTemplate.delete(List.of(keys));
    }

    /**
     * @see <a href="https://redis.io/commands/del">Redis Documentation: DEL</a>
     */
    public Long delete(Collection<String> keys){
        return stringRedisTemplate.delete(keys);
    }

    /**
     * @see <a href="https://redis.io/commands/pexpire">Redis Documentation: PEXPIRE</a>
     */
    public Boolean expire(String key, long timeout, TimeUnit unit){
        return stringRedisTemplate.expire(key, timeout, unit);
    }

    /**
     * @see <a href="https://redis.io/commands/ttl">Redis Documentation: TTL</a>
     */
    public Long getExpire(final String key){
        return stringRedisTemplate.getExpire(key);
    }

    /**
     * @see <a href="https://redis.io/commands/ttl">Redis Documentation: TTL</a>
     */
    public Long getExpire(final String key, TimeUnit timeUnit){
        return stringRedisTemplate.getExpire(key, timeUnit);
    }

    public Map<String, Object> pipeline(Map<String, java.util.function.Consumer<RedisOperations<String, Object>>> operationMap){
        List<String> keys = new ArrayList<>(operationMap.keySet());
        List<Object> results = stringRedisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            public Object execute(@NotNull RedisOperations redisOperations) {
                operationMap.forEach((key, consumer) -> consumer.accept(redisOperations));
                return null;
            }
        });
        return IntStream.range(0, keys.size()).boxed().collect(Collectors.toMap(keys::get, results::get));
    }

    public void luaClean(String pattern){
        DefaultRedisScript<Void> luaScript = new DefaultRedisScript<>();
        luaScript.setScriptText(LUA_CLEAN);
        luaScript.setResultType(Void.class);
        stringRedisTemplate.execute(luaScript, java.util.Collections.emptyList(), pattern);
    }

    public <T> T luaExec(String lua, Class<T> resultType, List<String> keys, Object... args){
        DefaultRedisScript<T> luaScript = new DefaultRedisScript<>();
        luaScript.setScriptText(lua);
        luaScript.setResultType(resultType);
        return stringRedisTemplate.execute(luaScript, keys, args);
    }

    /* ******************************************
     * opsForValue
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/get">Redis Documentation: GET</a>
     */
    public String getValue(String key){
        ValueOperations<String, String> operation = stringRedisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * @see <a href="https://redis.io/commands/get">Redis Documentation: GET</a>
     */
    public <T> T getValue(String key, Class<T> clazz){
        return readString(getValue(key), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/get">Redis Documentation: GET</a>
     */
    public <T> T getValue(String key, TypeReference<T> typeReference) {
        return readString(getValue(key), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/getdel">Redis Documentation: GETDEL</a>
     */
    public String getValueAndDelete(String key){
        ValueOperations<String, String> operation = stringRedisTemplate.opsForValue();
        return operation.getAndDelete(key);
    }

    /**
     * @see <a href="https://redis.io/commands/getdel">Redis Documentation: GETDEL</a>
     */
    public <T> T getValueAndDelete(String key, Class<T> clazz){
        return readString(getValueAndDelete(key), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/getdel">Redis Documentation: GETDEL</a>
     */
    public <T> T getValueAndDelete(String key, TypeReference<T> typeReference){
        return readString(getValueAndDelete(key), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/getset">Redis Documentation: GETSET</a>
     */
    public String getValueAndPut(String key, String value){
        ValueOperations<String, String> operation = stringRedisTemplate.opsForValue();
        return operation.getAndSet(key, value);
    }

    /**
     * @see <a href="https://redis.io/commands/getset">Redis Documentation: GETSET</a>
     */
    public <T> T getValueAndPut(String key, String value, Class<T> clazz){
        return readString(getValueAndPut(key, value), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/getset">Redis Documentation: GETSET</a>
     */
    public <T> T getValueAndPut(String key, String value, TypeReference<T> typeReference){
        return readString(getValueAndPut(key, value), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/getex">Redis Documentation: GETEX</a>
     */
    public String getValueAndExpire(String key, long timeout, TimeUnit timeUnit){
        ValueOperations<String, String> operation = stringRedisTemplate.opsForValue();
        return operation.getAndExpire(key, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/getex">Redis Documentation: GETEX</a>
     */
    public <T> T getValueAndExpire(String key, long timeout, TimeUnit timeUnit, Class<T> clazz){
        return readString(getValueAndExpire(key, timeout, timeUnit), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/getex">Redis Documentation: GETEX</a>
     */
    public <T> T getValueAndExpire(String key, long timeout, TimeUnit timeUnit, TypeReference<T> typeReference){
        return readString(getValueAndExpire(key, timeout, timeUnit), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/getex">Redis Documentation: GETEX</a>
     */
    public String getValueAndPersist(String key){
        ValueOperations<String, String> operation = stringRedisTemplate.opsForValue();
        return operation.getAndPersist(key);
    }

    /**
     * @see <a href="https://redis.io/commands/getex">Redis Documentation: GETEX</a>
     */
    public <T> T getValueAndPersist(String key, Class<T> clazz){
        return readString(getValueAndPersist(key), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/getex">Redis Documentation: GETEX</a>
     */
    public <T> T getValueAndPersist(String key, TypeReference<T> typeReference){
        return readString(getValueAndPersist(key), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/mget">Redis Documentation: MGET</a>
     */
    public List<String> getMultiValue(String... keys){
        ValueOperations<String, String> operation = stringRedisTemplate.opsForValue();
        return operation.multiGet(List.of(keys));
    }

    /**
     * @see <a href="https://redis.io/commands/mget">Redis Documentation: MGET</a>
     */
    public <T> List<T> getMultiValue(Class<T> clazz, String... keys){
        ValueOperations<String, String> operation = stringRedisTemplate.opsForValue();
        List<String> values = operation.multiGet(List.of(keys));
        if(CollectionUtils.isEmpty(values)){
            return java.util.Collections.emptyList();
        }
        return Collections.copyToList(values, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/mget">Redis Documentation: MGET</a>
     */
    public List<String> getMultiValue(Collection<String> keys){
        ValueOperations<String, String> operation = stringRedisTemplate.opsForValue();
        return operation.multiGet(keys);
    }

    /**
     * @see <a href="https://redis.io/commands/mget">Redis Documentation: MGET</a>
     */
    public <T> List<T> getMultiValue(Collection<String> keys, Class<T> clazz){
        ValueOperations<String, String> operation = stringRedisTemplate.opsForValue();
        List<String> values = operation.multiGet(keys);
        if(CollectionUtils.isEmpty(values)){
            return java.util.Collections.emptyList();
        }
        return Collections.copyToList(values, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
     */
    public <T> void putValue(String key, T value){
        stringRedisTemplate.opsForValue().set(key, writeString(value));
    }

    /**
     * @see <a href="https://redis.io/commands/setex">Redis Documentation: SETEX</a>
     */
    public <T> void putExpire(String key, T value, long timeout, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, writeString(value), timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/setnx">Redis Documentation: SETNX</a>
     */
    public <T> Boolean putValueIfAbsent(String key, T value){
        return stringRedisTemplate.opsForValue().setIfAbsent(key, writeString(value));
    }

    /**
     * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
     */
    public <T> Boolean putExpireIfAbsent(String key, T value, long timeout, TimeUnit timeUnit){
        return stringRedisTemplate.opsForValue().setIfAbsent(key, writeString(value), timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
     */
    public <T> Boolean putValueIfPresent(String key, T value){
        return stringRedisTemplate.opsForValue().setIfPresent(key, writeString(value));
    }

    /**
     * @see <a href="https://redis.io/commands/set">Redis Documentation: SET</a>
     */
    public <T> Boolean putExpireIfPresent(String key, T value, long timeout, TimeUnit timeUnit){
        return stringRedisTemplate.opsForValue().setIfPresent(key, writeString(value), timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/mset">Redis Documentation: MSET</a>
     */
    public void putMultiValue(Map<String, Object> map){
        if(map == null || map.isEmpty()) {
            return;
        }
        stringRedisTemplate.opsForValue().multiSet(Collections.copyToMap(map.entrySet(),
                Map.Entry::getKey, entry -> writeString(entry.getValue())));
    }

    /**
     * @see <a href="https://redis.io/commands/incrby">Redis Documentation: INCRBY</a>
     */
    public Long incrementValue(String key, int step){
        return stringRedisTemplate.opsForValue().increment(key, step);
    }

    /**
     * @see <a href="https://redis.io/commands/decrby">Redis Documentation: DECRBY</a>
     */
    public Long decrementValue(String key, int step){
        return stringRedisTemplate.opsForValue().decrement(key, step);
    }

    /* ******************************************
     * opsForHash
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/hlen">Redis Documentation: HLEN</a>
     */
    public Long sizeOfMap(String key) {
        return stringRedisTemplate.opsForHash().size(key);
    }

    /**
     * @see <a href="https://redis.io/commands/hscan">Redis Documentation: HSCAN</a>
     */
    public Cursor<Map.Entry<String, String>> scanMap(String key, ScanOptions scanOptions) {
        HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
        return hashOps.scan(key, scanOptions);
    }

    /**
     * @see <a href="https://redis.io/commands/hexits">Redis Documentation: HEXISTS</a>
     */
    public Boolean hasKeyInMap(String key, String hKey){
        return stringRedisTemplate.opsForHash().hasKey(key, hKey);
    }

    /**
     * @see <a href="https://redis.io/commands/hgetall">Redis Documentation: HGETALL</a>
     */
    public Map<String, String> getMap(String key){
        HashOperations<String, String, String> operations = stringRedisTemplate.opsForHash();
        return operations.entries(key);
    }

    /**
     * @see <a href="https://redis.io/commands/hgetall">Redis Documentation: HGETALL</a>
     */
    public <T> Map<String, T> getMap(String key, Class<T> clazz){
        HashOperations<String, String, String> operations = stringRedisTemplate.opsForHash();
        Map<String, String> mapValue = operations.entries(key);
        return Collections.copyToMap(
                mapValue.entrySet(), Map.Entry::getKey, entry -> readString(entry.getValue(), clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/hgetall">Redis Documentation: HGETALL</a>
     */
    public <T> Map<String, T> getMap(String key, TypeReference<T> typeReference){
        HashOperations<String, String, String> operations = stringRedisTemplate.opsForHash();
        Map<String, String> mapValue = operations.entries(key);
        return Collections.copyToMap(
                mapValue.entrySet(), Map.Entry::getKey, entry -> readString(entry.getValue(), typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/hget">Redis Documentation: HGET</a>
     */
    public String getMap(String key, String hKey){
        HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * @see <a href="https://redis.io/commands/hget">Redis Documentation: HGET</a>
     */
    public <T> T getMap(String key, String hKey, Class<T> clazz){
        HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();
        String value = opsForHash.get(key, hKey);
        if(value != null){
            return readString(value, clazz);
        }
        return null;
    }

    /**
     * @see <a href="https://redis.io/commands/hget">Redis Documentation: HGET</a>
     */
    public <T> T getMap(String key, String hKey, TypeReference<T> typeReference){
        HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();
        String value = opsForHash.get(key, hKey);
        if(value != null){
            return readString(value, typeReference);
        }
        return null;
    }

    /**
     * @see <a href="https://redis.io/commands/hmget">Redis Documentation: HMGET</a>
     */
    public List<String> getMultiMap(String key, Collection<String> hKeys){
        HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();
        return opsForHash.multiGet(key, hKeys);
    }

    /**
     * @see <a href="https://redis.io/commands/hmget">Redis Documentation: HMGET</a>
     */
    public <T> List<T> getMultiMap(String key, Collection<String> hKeys, Class<T> clazz){
        HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();
        List<String> values = opsForHash.multiGet(key, hKeys);
        return Collections.copyToList(values, value -> readString(value, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/hmget">Redis Documentation: HMGET</a>
     */
    public <T> List<T> getMultiMap(String key, Collection<String> hKeys, TypeReference<T> typeReference){
        HashOperations<String, String, String> opsForHash = stringRedisTemplate.opsForHash();
        List<String> values = opsForHash.multiGet(key, hKeys);
        return Collections.copyToList(values, value -> readString(value, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/hset">Redis Documentation: HSET</a>
     */
    public <T> void putMap(String key, String hKey, T value){
        stringRedisTemplate.opsForHash().put(key, hKey, writeString(value));
    }

    /**
     * @see <a href="https://redis.io/commands/hmset">Redis Documentation: HMSET</a>
     */
    public void putMap(String key, Map<String, Object> dataMap){
        if(dataMap == null || dataMap.isEmpty()) {
            return;
        }
        Map<String, String> stringMap = Collections.copyToMap(
                dataMap.entrySet(), Map.Entry::getKey, entry -> writeString(entry.getValue()));
        stringRedisTemplate.opsForHash().putAll(key, stringMap);
    }

    /**
     * @see <a href="https://redis.io/commands/hsetnx">Redis Documentation: HSETNX</a>
     */
    public <T> Boolean putMapIfAbsent(String key, String hKey, T value){
        HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
        return hashOps.putIfAbsent(key, hKey, writeString(value));
    }

    /**
     * @see <a href="https://redis.io/commands/hincrby">Redis Documentation: HINCRBY</a>
     */
    public Long incrementMap(String key, String hKey, long delta) {
        return stringRedisTemplate.opsForHash().increment(key, hKey, delta);
    }

    /**
     * @see <a href="https://redis.io/commands/hdel">Redis Documentation: HDEL</a>
     */
    public Long removeFromMap(String key, String hKey){
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        return hashOperations.delete(key, hKey);
    }

    /* ******************************************
     * opsForList
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/llen">Redis Documentation: LLEN</a>
     */
    public Long sizeOfList(String key) {
        return stringRedisTemplate.opsForList().size(key);
    }

    /**
     * @see <a href="https://redis.io/commands/lpos">Redis Documentation: LPOS</a>
     */
    public <T> Long indexOfList(String key, T value){
        return stringRedisTemplate.opsForList().indexOf(key, writeString(value));
    }

    /**
     * @see <a href="https://redis.io/commands/lpos">Redis Documentation: LPOS</a>
     */
    public <T> Long lastIndexOfList(String key, T value){
        return stringRedisTemplate.opsForList().lastIndexOf(key, writeString(value));
    }

    /**
     * @see <a href="https://redis.io/commands/lindex">Redis Documentation: LINDEX</a>
     */
    public String indexValueOfList(String key, long index){
        return stringRedisTemplate.opsForList().index(key, index);
    }

    /**
     * @see <a href="https://redis.io/commands/lindex">Redis Documentation: LINDEX</a>
     */
    public <T> T indexValueOfList(String key, long index, Class<T> clazz){
        String value = stringRedisTemplate.opsForList().index(key, index);
        return readString(value, clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/lrange">Redis Documentation: LRANGE</a>
     */
    public List<String> rangeOfList(String key, int start, int end){
        return stringRedisTemplate.opsForList().range(key, start, end);
    }

    /**
     * @see <a href="https://redis.io/commands/lrange">Redis Documentation: LRANGE</a>
     */
    public <T> List<T> rangeOfList(String key, int start, int end, Class<T> clazz){
        List<String> values = stringRedisTemplate.opsForList().range(key, start, end);
        return Collections.copyToList(values, value -> readString(value, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/lrange">Redis Documentation: LRANGE</a>
     */
    public <T> List<T> rangeOfList(String key, int start, int end, TypeReference<T> typeReference){
        List<String> values = stringRedisTemplate.opsForList().range(key, start, end);
        return Collections.copyToList(values, value -> readString(value, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/lset">Redis Documentation: LSET</a>
     */
    public <T> void insertListByIndex(String key, long index, T value){
        stringRedisTemplate.opsForList().set(key, index, writeString(value));
    }

    /**
     * @see <a href="https://redis.io/commands/linsert">Redis Documentation: LINSERT</a>
     */
    public <T> long insertListBefore(String key, T pivot, T value){
        Long count = stringRedisTemplate.opsForList().leftPush(key, writeString(pivot), writeString(value));
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/linsert">Redis Documentation: LINSERT</a>
     */
    public <T> long insertListAfter(String key, T pivot, T value){
        Long count = stringRedisTemplate.opsForList().rightPush(key, writeString(pivot), writeString(value));
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/lpush">Redis Documentation: LPUSH</a>
     */
    public <T> long pushListFromLeft(String key, T value){
        Long count = stringRedisTemplate.opsForList().leftPush(key, writeString(value));
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/lpushx">Redis Documentation: LPUSHX</a>
     */
    public <T> long pushListFromLeftIfPresent(String key, T value){
        Long count = stringRedisTemplate.opsForList().leftPushIfPresent(key, writeString(value));
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/lpush">Redis Documentation: LPUSH</a>
     */
    public long pushAllListFromLeft(String key, Object... values){
        Long count = stringRedisTemplate.opsForList().leftPushAll(key, Collections.arrayToList(values, this::writeString));
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/lpush">Redis Documentation: LPUSH</a>
     */
    public long pushAllListFromLeft(String key, Collection<Object> values){
        Long count = stringRedisTemplate.opsForList().leftPushAll(key, Collections.copyToList(values, this::writeString));
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/rpush">Redis Documentation: RPUSH</a>
     */
    public <T> long pushListFromRight(String key, T value){
        Long count = stringRedisTemplate.opsForList().rightPush(key, writeString(value));
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/rpushx">Redis Documentation: RPUSHX</a>
     */
    public <T> long pushListFromRightIfPresent(String key, T value){
        Long count = stringRedisTemplate.opsForList().rightPushIfPresent(key, writeString(value));
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/rpush">Redis Documentation: RPUSH</a>
     */
    public long pushAllListFromRight(String key, Object... values){
        Long count = stringRedisTemplate.opsForList().rightPushAll(key, Collections.arrayToList(values, this::writeString));
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/rpush">Redis Documentation: RPUSH</a>
     */
    public long pushAllListFromRight(String key, Collection<Object> values){
        Long count = stringRedisTemplate.opsForList().rightPushAll(key, Collections.copyToList(values, this::writeString));
        return count == null ? 0 : count;
    }

    /**
     * @see <a href="https://redis.io/commands/lpop">Redis Documentation: LPOP</a>
     */
    public String popListFromLeft(String key){
        return stringRedisTemplate.opsForList().leftPop(key);
    }

    /**
     * @see <a href="https://redis.io/commands/lpop">Redis Documentation: LPOP</a>
     */
    public <T> T popListFromLeft(String key, Class<T> clazz){
        return readString(popListFromLeft(key), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/lpop">Redis Documentation: LPOP</a>
     */
    public <T> T popListFromLeft(String key, TypeReference<T> typeReference){
        return readString(popListFromLeft(key), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/blpop">Redis Documentation: BLPOP</a>
     */
    public String popListFromLeft(String key, long timeout, TimeUnit timeUnit){
        return stringRedisTemplate.opsForList().leftPop(key, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/blpop">Redis Documentation: BLPOP</a>
     */
    public <T> T popListFromLeft(String key, long timeout, TimeUnit timeUnit, Class<T> clazz){
        return readString(popListFromLeft(key, timeout, timeUnit), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/blpop">Redis Documentation: BLPOP</a>
     */
    public <T> T popListFromLeft(String key, long timeout, TimeUnit timeUnit, TypeReference<T> typeReference){
        return readString(popListFromLeft(key, timeout, timeUnit), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/rpop">Redis Documentation: RPOP</a>
     */
    public String popListFromRight(String key){
        return stringRedisTemplate.opsForList().rightPop(key);
    }

    /**
     * @see <a href="https://redis.io/commands/rpop">Redis Documentation: RPOP</a>
     */
    public <T> T popListFromRight(String key, Class<T> clazz){
        return readString(popListFromRight(key), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/rpop">Redis Documentation: RPOP</a>
     */
    public <T> T popListFromRight(String key, TypeReference<T> typeReference){
        return readString(popListFromRight(key), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/brpop">Redis Documentation: BRPOP</a>
     */
    public String popListFromRight(String key, long timeout, TimeUnit timeUnit){
        return stringRedisTemplate.opsForList().rightPop(key, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/brpop">Redis Documentation: BRPOP</a>
     */
    public <T> T popListFromRight(String key, long timeout, TimeUnit timeUnit, Class<T> clazz){
        return readString(popListFromRight(key, timeout, timeUnit), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/brpop">Redis Documentation: BRPOP</a>
     */
    public <T> T popListFromRight(String key, long timeout, TimeUnit timeUnit, TypeReference<T> typeReference){
        return readString(popListFromRight(key, timeout, timeUnit), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/rpoplpush">Redis Documentation: RPOPLPUSH</a>
     */
    public String popListFromRightToLeft(String rightKey, String leftKey){
        return stringRedisTemplate.opsForList().rightPopAndLeftPush(rightKey, leftKey);
    }

    /**
     * @see <a href="https://redis.io/commands/rpoplpush">Redis Documentation: RPOPLPUSH</a>
     */
    public <T> T popListFromRightToLeft(String rightKey, String leftKey, Class<T> clazz){
        return readString(popListFromRightToLeft(rightKey, leftKey), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/rpoplpush">Redis Documentation: RPOPLPUSH</a>
     */
    public <T> T popListFromRightToLeft(String rightKey, String leftKey, TypeReference<T> typeReference){
        return readString(popListFromRightToLeft(rightKey, leftKey), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/brpoplpush">Redis Documentation: BRPOPLPUSH</a>
     */
    public String popListFromRightToLeft(String rightKey, String leftKey, long timeout, TimeUnit timeUnit){
        return stringRedisTemplate.opsForList().rightPopAndLeftPush(rightKey, leftKey, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/brpoplpush">Redis Documentation: BRPOPLPUSH</a>
     */
    public <T> T popListFromRightToLeft(String rightKey, String leftKey, long timeout, TimeUnit timeUnit, Class<T> clazz){
        return readString(popListFromRightToLeft(rightKey, leftKey, timeout, timeUnit), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/brpoplpush">Redis Documentation: BRPOPLPUSH</a>
     */
    public <T> T popListFromRightToLeft(String rightKey, String leftKey, long timeout, TimeUnit timeUnit, TypeReference<T> typeReference){
        return readString(popListFromRightToLeft(rightKey, leftKey, timeout, timeUnit), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/lmove">Redis Documentation: LMOVE</a>
     */
    public String moveList(String srcKey, RedisListCommands.Direction from, String destKey, RedisListCommands.Direction to){
        return stringRedisTemplate.opsForList().move(srcKey, from, destKey, to);
    }

    /**
     * @see <a href="https://redis.io/commands/lmove">Redis Documentation: LMOVE</a>
     */
    public <T> T moveList(String srcKey, RedisListCommands.Direction from, String destKey, RedisListCommands.Direction to, Class<T> clazz){
        return readString(moveList(srcKey, from, destKey, to), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/lmove">Redis Documentation: LMOVE</a>
     */
    public <T> T moveList(String srcKey, RedisListCommands.Direction from, String destKey, RedisListCommands.Direction to, TypeReference<T> typeReference){
        return readString(moveList(srcKey, from, destKey, to), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/blmove">Redis Documentation: BLMOVE</a>
     */
    public String moveList(String srcKey, RedisListCommands.Direction from, String destKey, RedisListCommands.Direction to, long timeout, TimeUnit timeUnit){
        return stringRedisTemplate.opsForList().move(srcKey, from, destKey, to, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/blmove">Redis Documentation: BLMOVE</a>
     */
    public <T> T moveList(String srcKey, RedisListCommands.Direction from, String destKey, RedisListCommands.Direction to, long timeout, TimeUnit timeUnit, Class<T> clazz){
        return readString(moveList(srcKey, from, destKey, to, timeout, timeUnit), clazz);
    }

    /**
     * @see <a href="https://redis.io/commands/blmove">Redis Documentation: BLMOVE</a>
     */
    public <T> T moveList(String srcKey, RedisListCommands.Direction from, String destKey, RedisListCommands.Direction to, long timeout, TimeUnit timeUnit, TypeReference<T> typeReference){
        return readString(moveList(srcKey, from, destKey, to, timeout, timeUnit), typeReference);
    }

    /**
     * @see <a href="https://redis.io/commands/lrem">Redis Documentation: LREM</a>
     */
    public <T> Long removeFromList(String key, T value, long count){
        return stringRedisTemplate.opsForList().remove(key, count, writeString(value));
    }

    /* ******************************************
     * opsForSet
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/scard">Redis Documentation: SCARD</a>
     */
    public Long sizeOfSet(String key) {
        return stringRedisTemplate.opsForSet().size(key);
    }

    /**
     * @see <a href="https://redis.io/commands/scan">Redis Documentation: SCAN</a>
     */
    public Cursor<String> scanSet(String key, ScanOptions scanOptions) {
        return stringRedisTemplate.opsForSet().scan(key, scanOptions);
    }

    /**
     * @see <a href="https://redis.io/commands/sismember">Redis Documentation: SISMEMBER</a>
     */
    public <T> Boolean memberOfSet(String key, T member) {
        return stringRedisTemplate.opsForSet().isMember(key, writeString(member));
    }

    /**
     * @see <a href="https://redis.io/commands/smembers">Redis Documentation: SMEMBERS</a>
     */
    public Set<String> getSet(String key){
        return stringRedisTemplate.opsForSet().members(key);
    }

    /**
     * @see <a href="https://redis.io/commands/smembers">Redis Documentation: SMEMBERS</a>
     */
    public <T> Set<T> getSet(String key, Class<T> clazz){
        Set<String> values = stringRedisTemplate.opsForSet().members(key);
        return Collections.copyToSet(values, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/smembers">Redis Documentation: SMEMBERS</a>
     */
    public <T> Set<T> getSet(String key, TypeReference<T> typeReference){
        Set<String> values = stringRedisTemplate.opsForSet().members(key);
        return Collections.copyToSet(values, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/sadd">Redis Documentation: SADD</a>
     */
    public void offerSet(String key, Set<Object> values){
        BoundSetOperations<String, String> setOperation = stringRedisTemplate.boundSetOps(key);
        for (Object v : values) {
            setOperation.add(writeString(v));
        }
    }

    /**
     * @see <a href="https://redis.io/commands/sadd">Redis Documentation: SADD</a>
     */
    public void offerSet(String key, Object... values){
        BoundSetOperations<String, String> setOperation = stringRedisTemplate.boundSetOps(key);
        for (Object v : values) {
            setOperation.add(writeString(v));
        }
    }

    /**
     * @see <a href="https://redis.io/commands/spop">Redis Documentation: SPOP</a>
     */
    public List<String> popSet(String key, int count){
        return stringRedisTemplate.opsForSet().pop(key, count);
    }

    /**
     * @see <a href="https://redis.io/commands/spop">Redis Documentation: SPOP</a>
     */
    public <T> List<T> popSet(String key, int count, Class<T> clazz){
        List<String> list = stringRedisTemplate.opsForSet().pop(key, count);
        return Collections.copyToList(list, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/spop">Redis Documentation: SPOP</a>
     */
    public <T> List<T> popSet(String key, int count, TypeReference<T> typeReference){
        List<String> list = stringRedisTemplate.opsForSet().pop(key, count);
        return Collections.copyToList(list, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public Set<String> intersectSet(Collection<String> keys){
        return stringRedisTemplate.opsForSet().intersect(keys);
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public <T> Set<T> intersectSet(Collection<String> keys, Class<T> clazz){
        Set<String> set = stringRedisTemplate.opsForSet().intersect(keys);
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public <T> Set<T> intersectSet(Collection<String> keys, TypeReference<T> typeReference){
        Set<String> set = stringRedisTemplate.opsForSet().intersect(keys);
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public Set<String> intersectSet(String key, Collection<String> others){
        return stringRedisTemplate.opsForSet().intersect(key, others);
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public <T> Set<T> intersectSet(String key, Collection<String> others, Class<T> clazz){
        Set<String> set = stringRedisTemplate.opsForSet().intersect(key, others);
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public <T> Set<T> intersectSet(String key, Collection<String> others, TypeReference<T> typeReference){
        Set<String> set = stringRedisTemplate.opsForSet().intersect(key, others);
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public Set<String> intersectSet(String key, String... others){
        return stringRedisTemplate.opsForSet().intersect(key, List.of(others));
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public <T> Set<T> intersectSet(Class<T> clazz, String key, String... others){
        Set<String> set = stringRedisTemplate.opsForSet().intersect(key, List.of(others));
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/sinter">Redis Documentation: SINTER</a>
     */
    public <T> Set<T> intersectSet(TypeReference<T> typeReference, String key, String... others){
        Set<String> set = stringRedisTemplate.opsForSet().intersect(key, List.of(others));
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/sinterstore">Redis Documentation: SINTERSTORE</a>
     */
    public Long intersectSetAndStore(String destKey, Collection<String> keys){
        return stringRedisTemplate.opsForSet().intersectAndStore(keys, destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/sinterstore">Redis Documentation: SINTERSTORE</a>
     */
    public Long intersectSetAndStore(String destKey, String... keys){
        return stringRedisTemplate.opsForSet().intersectAndStore(List.of(keys), destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public Set<String> unionSet(Collection<String> keys){
        return stringRedisTemplate.opsForSet().union(keys);
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public <T> Set<T> unionSet(Collection<String> keys, Class<T> clazz){
        Set<String> set = stringRedisTemplate.opsForSet().union(keys);
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public <T> Set<T> unionSet(Collection<String> keys, TypeReference<T> typeReference){
        Set<String> set = stringRedisTemplate.opsForSet().union(keys);
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public Set<String> unionSet(String key, Collection<String> others){
        return stringRedisTemplate.opsForSet().union(key, others);
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public <T> Set<T> unionSet(String key, Collection<String> others, Class<T> clazz){
        Set<String> set = stringRedisTemplate.opsForSet().union(key, others);
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public <T> Set<T> unionSet(String key, Collection<String> others, TypeReference<T> typeReference){
        Set<String> set = stringRedisTemplate.opsForSet().union(key, others);
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public Set<String> unionSet(String key, String... others){
        return stringRedisTemplate.opsForSet().union(key, List.of(others));
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public <T> Set<T> unionSet(Class<T> clazz, String key, String... others){
        Set<String> set = stringRedisTemplate.opsForSet().union(key, List.of(others));
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/sunion">Redis Documentation: SUNION</a>
     */
    public <T> Set<T> unionSet(TypeReference<T> typeReference, String key, String... others){
        Set<String> set = stringRedisTemplate.opsForSet().union(key, List.of(others));
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/sunionstore">Redis Documentation: SUNIONSTORE</a>
     */
    public Long unionSetAndStore(String destKey, Collection<String> keys){
        return stringRedisTemplate.opsForSet().unionAndStore(keys, destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/sunionstore">Redis Documentation: SUNIONSTORE</a>
     */
    public Long unionSetAndStore(String destKey, String... keys){
        return stringRedisTemplate.opsForSet().unionAndStore(List.of(keys), destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/sdiff">Redis Documentation: SDIFF</a>
     */
    public Set<String> diffSet(String key, String... others){
        return stringRedisTemplate.opsForSet().difference(key, List.of(others));
    }

    /**
     * @see <a href="https://redis.io/commands/sdiff">Redis Documentation: SDIFF</a>
     */
    public <T> Set<T> diffSet(String key, Collection<String> others, Class<T> clazz){
        Set<String> set = stringRedisTemplate.opsForSet().difference(key, others);
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/sdiff">Redis Documentation: SDIFF</a>
     */
    public <T> Set<T> diffSet(String key, Collection<String> others, TypeReference<T> typeReference){
        Set<String> set = stringRedisTemplate.opsForSet().difference(key, others);
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/sdiff">Redis Documentation: SDIFF</a>
     */
    public Set<String> diffSet(String key, Collection<String> others){
        return stringRedisTemplate.opsForSet().difference(key, others);
    }

    /**
     * @see <a href="https://redis.io/commands/sdiff">Redis Documentation: SDIFF</a>
     */
    public <T> Set<T> diffSet(Class<T> clazz, String key, String... others){
        Set<String> set = stringRedisTemplate.opsForSet().difference(key, List.of(others));
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/sdiff">Redis Documentation: SDIFF</a>
     */
    public <T> Set<T> diffSet(TypeReference<T> typeReference, String key, String... others){
        Set<String> set = stringRedisTemplate.opsForSet().difference(key, List.of(others));
        if(CollectionUtils.isEmpty(set)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(set, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/sdiffstore">Redis Documentation: SDIFFSTORE</a>
     */
    public Long diffSetAndStore(String destKey, String key, String... others){
        return stringRedisTemplate.opsForSet().differenceAndStore(key, List.of(others), destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/sdiffstore">Redis Documentation: SDIFFSTORE</a>
     */
    public Long diffSetAndStore(String destKey, String key, Collection<String> others){
        return stringRedisTemplate.opsForSet().differenceAndStore(key, others, destKey);
    }

    /**
     * @see <a href="https://redis.io/commands/srem">Redis Documentation: SREM</a>
     */
    public Long removeFromSet(String key, Object... values){
        BoundSetOperations<String, String> setOperation = stringRedisTemplate.boundSetOps(key);
        String[] array= Collections.arrayToList(values, this::writeString).toArray(String[]::new);
        return setOperation.remove((Object[]) array);
    }

    /* ******************************************
     * opsForZSet
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/zcard">Redis Documentation: ZCARD</a>
     */
    public Long sizeOfZset(String key) {
        return stringRedisTemplate.opsForZSet().size(key);
    }

    /**
     * @see <a href="https://redis.io/commands/zcount">Redis Documentation: ZCOUNT</a>
     */
    public Long countZsetByScore(String key, double min, double max) {
        return stringRedisTemplate.opsForZSet().count(key, min, max);
    }

    /**
     * @see <a href="https://redis.io/commands/zrank">Redis Documentation: ZRANK</a>
     */
    public <T> Long rankOfZset(String key, T value){
        return stringRedisTemplate.opsForZSet().rank(key, writeString(value));
    }

    /**
     * @see <a href="https://redis.io/commands/zscore">Redis Documentation: ZSCORE</a>
     */
    public <T> Boolean memberOfZset(String key, T value) {
        return stringRedisTemplate.opsForZSet().score(key, writeString(value)) != null;
    }

    /**
     * @see <a href="https://redis.io/commands/zrange">Redis Documentation: ZRANGE</a>
     */
    public String firstOfZset(String key){
        Set<String> set = stringRedisTemplate.opsForZSet().range(key, 0, 0);
        if (set != null && !set.isEmpty()) {
            return set.iterator().next();
        }
        return null;
    }

    /**
     * @see <a href="https://redis.io/commands/zrange">Redis Documentation: ZRANGE</a>
     */
    public <T> T firstOfZset(String key, Class<T> clazz){
        Set<String> set = stringRedisTemplate.opsForZSet().range(key, 0, 0);
        if (set != null && !set.isEmpty()) {
            return readString(set.iterator().next(), clazz);
        }
        return null;
    }

    /**
     * @see <a href="https://redis.io/commands/zrange">Redis Documentation: ZRANGE</a>
     */
    public <T> T firstOfZset(String key, TypeReference<T> typeReference){
        Set<String> set = stringRedisTemplate.opsForZSet().range(key, 0, 0);
        if (set != null && !set.isEmpty()) {
            return readString(set.iterator().next(), typeReference);
        }
        return null;
    }

    /**
     * @see <a href="https://redis.io/commands/zrange">Redis Documentation: ZRANGE</a>
     */
    public Set<String> rangeOfZset(String key, long start, long end) {
        return stringRedisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * @see <a href="https://redis.io/commands/zrange">Redis Documentation: ZRANGE</a>
     */
    public <T> Set<T> rangeOfZset(String key, long start, long end, Class<T> clazz){
        Set<String> values = stringRedisTemplate.opsForZSet().range(key, start, end);
        if(CollectionUtils.isEmpty(values)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(values, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/zrange">Redis Documentation: ZRANGE</a>
     */
    public <T> Set<T> rangeOfZset(String key, long start, long end, TypeReference<T> typeReference){
        Set<String> values = stringRedisTemplate.opsForZSet().range(key, start, end);
        if(CollectionUtils.isEmpty(values)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(values, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis Documentation: ZRANGEBYSCORE</a>
     */
    public Set<String> rangeOfZsetByScore(String key, double min, double max){
        return stringRedisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    /**
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis Documentation: ZRANGEBYSCORE</a>
     */
    public <T> Set<T> rangeOfZsetByScore(String key, double min, double max, Class<T> clazz){
        Set<String> values = stringRedisTemplate.opsForZSet().rangeByScore(key, min, max);
        if(CollectionUtils.isEmpty(values)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(values, v -> readString(v, clazz));
    }

    /**
     * @see <a href="https://redis.io/commands/zrangebyscore">Redis Documentation: ZRANGEBYSCORE</a>
     */
    public <T> Set<T> rangeOfZsetByScore(String key, double min, double max, TypeReference<T> typeReference){
        Set<String> values = stringRedisTemplate.opsForZSet().rangeByScore(key, min, max);
        if(CollectionUtils.isEmpty(values)){
            return java.util.Collections.emptySet();
        }
        return Collections.copyToSet(values, v -> readString(v, typeReference));
    }

    /**
     * @see <a href="https://redis.io/commands/zpopmin">Redis Documentation: ZPOPMIN</a>
     */
    public Set<ZSetOperations.TypedTuple<String>> popMinOfZset(final String key, int count){
        return stringRedisTemplate.opsForZSet().popMin(key, count);
    }

    /**
     * @see <a href="https://redis.io/commands/zpopmin">Redis Documentation: ZPOPMIN</a>
     */
    public ZSetOperations.TypedTuple<String> popMinOfZset(String key){
        return stringRedisTemplate.opsForZSet().popMin(key);
    }

    /**
     * @see <a href="https://redis.io/commands/bzpopmin">Redis Documentation: BZPOPMIN</a>
     */
    public ZSetOperations.TypedTuple<String> popMinOfZset(String key, long timeout, TimeUnit timeUnit){
        return stringRedisTemplate.opsForZSet().popMin(key, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/zpopmin">Redis Documentation: ZPOPMAX</a>
     */
    public Set<ZSetOperations.TypedTuple<String>> popMaxOfZset(String key, int count){
        return stringRedisTemplate.opsForZSet().popMax(key, count);
    }

    /**
     * @see <a href="https://redis.io/commands/zpopmin">Redis Documentation: ZPOPMAX</a>
     */
    public ZSetOperations.TypedTuple<String> popMaxOfZset(String key){
        return stringRedisTemplate.opsForZSet().popMax(key);
    }

    /**
     * @see <a href="https://redis.io/commands/bzpopmin">Redis Documentation: BZPOPMAX</a>
     */
    public ZSetOperations.TypedTuple<String> popMaxOfZset(String key, long timeout, TimeUnit timeUnit){
        return stringRedisTemplate.opsForZSet().popMax(key, timeout, timeUnit);
    }

    /**
     * @see <a href="https://redis.io/commands/zadd">Redis Documentation: ZADD</a>
     */
    public <T> void putZset(String key, T value, double score){
        stringRedisTemplate.opsForZSet().add(key, writeString(value), score);
    }

    /**
     * @see <a href="https://redis.io/commands/zadd">Redis Documentation: ZADD</a>
     */
    public <T> void putZset(final String key, Set<ZSetOperations.TypedTuple<String>> tuples){
        stringRedisTemplate.opsForZSet().add(key, tuples);
    }

    /**
     * @see <a href="https://redis.io/commands/zrem">Redis Documentation: ZREM</a>
     */
    public Long removeFromZset(String key, Object... values){
        return stringRedisTemplate.opsForZSet().remove(key, values);
    }

    /**
     * @see <a href="https://redis.io/commands/zremrangebyscore">Redis Documentation: ZREMRANGEBYSCORE</a>
     */
    public Long removeFromZsetByScore(String key, double min, double max){
        return stringRedisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    /* ******************************************
     * opsForStream
     * ******************************************/

    public StreamInfo.XInfoStream streamInfo(String key){
        return stringRedisTemplate.opsForStream().info(key);
    }

    public String createStreamGroup(String key, String group) {
        return stringRedisTemplate.opsForStream().createGroup(key, group);
    }

    /**
     * @see <a href="https://redis.io/commands/xadd">Redis Documentation: XADD</a>
     */
    public <T> RecordId publishStream(Record<String, T> record) {
        return stringRedisTemplate.opsForStream().add(record);
    }

    /**
     * @see <a href="https://redis.io/commands/xread">Redis Documentation: XREAD</a>
     */
    public List<MapRecord<String, Object, Object>> subscribeStream(Consumer consumer, StreamReadOptions readOptions, StreamOffset<String>... streams){
        return stringRedisTemplate.opsForStream().read(consumer, readOptions, streams);
    }

    /**
     * @see <a href="https://redis.io/commands/xread">Redis Documentation: XREAD</a>
     */
    public <V> List<ObjectRecord<String, V>> subscribeStream(Class<V> targetType, Consumer consumer, StreamReadOptions readOptions, StreamOffset<String>... streams) {
        return stringRedisTemplate.opsForStream().read(targetType, consumer, readOptions, streams);
    }

    /**
     * @see <a href="https://redis.io/commands/xack">Redis Documentation: XACK</a>
     */
    public Long ackStream(String key, String group, RecordId... recordIds){
        return stringRedisTemplate.opsForStream().acknowledge(key, group, recordIds);
    }

    /**
     * @see <a href="https://redis.io/commands/xdel">Redis Documentation: XDEL</a>
     */
    public Long deleteFromStream(String key, RecordId... recordIds){
        return stringRedisTemplate.opsForStream().delete(key, recordIds);
    }

    /* ******************************************
     * channel
     * ******************************************/

    /**
     * @see <a href="https://redis.io/commands/publish">Redis Documentation: PUBLISH</a>
     */
    public <T> void sendChannel(String channel, T message) {
        stringRedisTemplate.convertAndSend(channel, writeString(message));
    }

    private <T> T readString(String value, Class<T> clazz) {
        try {
            return MAPPER.readValue(value, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("json read failed", e);
        }
    }

    private <T> T readString(String value, TypeReference<T> typeReference) {
        try {
            return MAPPER.readValue(value, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("json read failed", e);
        }
    }

    private String writeString(Object value) {
        Asserts.notNull(value, "redis value can't be bull");
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("json write failed", e);
        }
    }
}
