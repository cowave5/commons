/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis.dict;

import java.util.*;

import com.cowave.commons.client.http.asserts.HttpHintException;
import com.cowave.commons.framework.helper.redis.RedisHelper;
import com.cowave.commons.framework.helper.redis.StringRedisHelper;
import org.apache.commons.lang3.StringUtils;

import lombok.RequiredArgsConstructor;

import static com.cowave.commons.client.http.constants.HttpCode.BAD_REQUEST;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
public class DictHelper {

    private final RedisHelper redisHelper;

    private final StringRedisHelper stringRedisHelper;

    private String getGroupKey(String... prefixes) {
        String prefix = "";
        if (prefixes != null && prefixes.length > 0) {
            prefix = String.join(":", prefixes) + ":";
        }
        return prefix + "dict:group:";
    }

    private String getTypeKey(String... prefixes){
        String prefix = "";
        if (prefixes != null && prefixes.length > 0) {
            prefix = String.join(":", prefixes) + ":";
        }
        return prefix + "dict:type:";
    }

    private String getDictKey(String... prefixes){
        String prefix = "";
        if (prefixes != null && prefixes.length > 0) {
            prefix = String.join(":", prefixes) + ":";
        }
        return prefix + "dict:code:";
    }

    /**
     * 清空字典
     */
    public void clear(String... prefixes) {
        String prefix = "";
        if (prefixes != null && prefixes.length > 0) {
            prefix = String.join(":", prefixes) + ":";
        }
        stringRedisHelper.luaClean(prefix + "dict:*");
    }

    /**
     * 存入字典缓存
     */
    public void put(Dict dict, String... prefixes) {
        if(dict.getGroupCode() == null){
            throw new HttpHintException(BAD_REQUEST, "{frame.dict.notnull.groupcode}");
        }
        if(dict.getTypeCode() == null){
            throw new HttpHintException(BAD_REQUEST, "{frame.dict.notnull.typecode}");
        }
        if(dict.getDictCode() == null){
            throw new HttpHintException(BAD_REQUEST, "{frame.dict.notnull.code}");
        }

        Object dictValue = CustomValueParser.getValue(dict.getDictValue(), dict.getValueType(), dict.getValueParser());
        dict.setDictValue(dictValue);

        if(!"root".equals(dict.getTypeCode())){
            redisHelper.putMap(getGroupKey(prefixes) + dict.getGroupCode(), dict.getDictCode(), dict);
            if(!"root".equals(dict.getGroupCode())){
                redisHelper.putMap(getTypeKey(prefixes) + dict.getTypeCode(), dict.getDictCode(), dict);
            }
        }
        redisHelper.putValue(getDictKey(prefixes) + dict.getDictCode(), dict);
    }

    /**
     * 获取某个分组字典
     */
    public <T extends Dict> List<T> getGroup(String groupCode, String... prefixes) {
        if(StringUtils.isBlank(groupCode)){
            return new ArrayList<>();
        }
        Map<String, T> map = redisHelper.getMap(getGroupKey(prefixes) + groupCode);
        List<T> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingInt(Dict::getDictOrder));
        return list;
    }

    /**
     * 获取某个类型字典
     */
    public <T extends Dict> List<T> getType(String typeCode, String... prefixes) {
        if(StringUtils.isBlank(typeCode)){
            return new ArrayList<>();
        }
        Map<String, T> map = redisHelper.getMap(getTypeKey(prefixes) + typeCode);
        List<T> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingInt(Dict::getDictOrder));
        return list;
    }

    /**
     * 获取字典
     */
    public <T extends Dict> T getDict(String dictCode, String... prefixes) {
        if(StringUtils.isBlank(dictCode)){
            return null;
        }
        return redisHelper.getValue(getDictKey(prefixes) + dictCode);
    }

    /**
     * 获取字典Label
     */
    public String getDictName(String dictCode) {
        Dict dict = getDict(dictCode);
        if(dict == null){
            return null;
        }
        return dict.getDictName();
    }

    /**
     * 获取字典值
     */
    public <T> T getDictValue(String dictCode) {
        Dict dict = getDict(dictCode);
        if(dict == null){
            return null;
        }
        return (T)dict.getDictValue();
    }

    /**
     * 删除字典
     */
    public void removeDict(String dictCode, String... prefixes) {
        if(StringUtils.isBlank(dictCode)){
            return;
        }
        Dict dict = redisHelper.getValue(getDictKey(prefixes) + dictCode);
        if(dict == null){
            return;
        }
        redisHelper.delete(getDictKey(prefixes) + dictCode);
        redisHelper.removeFromMap(getTypeKey(prefixes) + dict.getTypeCode(), dictCode);
        redisHelper.removeFromMap(getGroupKey(prefixes) + dict.getGroupCode(), dictCode);
    }

    /**
     * 删除类型
     *
     * <p>删除sys-dict:dict:{dictCode}
     * <p>从sys-dict:dict:{groupCode}中删除
     *
     * <p>删除sys-dict:type:{typeCode}
     * <p>从sys-dict:group:group中删除类型
     */
    public void removeType(String typeCode, String... prefixes) {
        if(StringUtils.isBlank(typeCode)){
            return;
        }
        Map<String, Dict> dictMap = redisHelper.getMap(getTypeKey(prefixes) + typeCode);
        for (Dict dict : dictMap.values()) {
            redisHelper.delete(getDictKey(prefixes) + dict.getDictCode());
            redisHelper.removeFromMap(getGroupKey(prefixes) + dict.getGroupCode(), dict.getDictCode());
        }
        redisHelper.delete(getTypeKey(prefixes) + typeCode);
        redisHelper.removeFromMap(getGroupKey(prefixes) + "group", typeCode);
    }

    /**
     * 删除分组
     *
     * <p>删除sys-dict:type:{groupCode}
     * <p>从sys-dict:group:group中删除类型
     *
     * <p>删除sys-dict:dict:{dictCode}
     *
     * <p>删除sys-dict:group:{groupCode}
     * <p>从sys-dict:group:root中删除分组
     */
    public void removeGroup(String groupCode, String... prefixes) {
        if(StringUtils.isBlank(groupCode)){
            return;
        }
        Map<String, Dict> typeMap = redisHelper.getMap(getTypeKey(prefixes) + groupCode);
        redisHelper.delete(getTypeKey(prefixes) + groupCode);
        for (Dict type : typeMap.values()) {
            redisHelper.removeFromMap(getGroupKey(prefixes) + "group", type.getDictCode());
        }

        Map<String, Dict> dictMap = redisHelper.getMap(getGroupKey(prefixes) + groupCode);
        for (Dict dict : dictMap.values()) {
            redisHelper.delete(getDictKey(prefixes) + dict.getDictCode());
        }
        redisHelper.delete(getGroupKey(prefixes) + groupCode);
        redisHelper.removeFromMap(getGroupKey(prefixes) + "root", groupCode);
    }
}
