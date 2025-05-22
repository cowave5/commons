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
import com.cowave.commons.framework.configuration.ApplicationProperties;
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

    private final ApplicationProperties applicationProperties;

    private final RedisHelper redisHelper;

    private final StringRedisHelper stringRedisHelper;

    private String getGroupKey(){
        return applicationProperties.getDictNamespace() + "group:";
    }

    private String getTypeKey(){
        return applicationProperties.getDictNamespace() + "type:";
    }

    private String getDictKey(){
        return applicationProperties.getDictNamespace() + "code:";
    }

    /**
     * 清空字典
     */
    public void clear() {
        String namespace = applicationProperties.getNamespace();
        if(StringUtils.isBlank(namespace)){
            namespace = "dict:*:";
        }else if(namespace.endsWith(":")){
            namespace = namespace + "dict:*";
        }else{
            namespace = namespace + ":dict:*";
        }
        stringRedisHelper.luaClean(namespace);
    }

    /**
     * 存入字典缓存
     */
    public void put(Dict dict) {
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
            redisHelper.putMap(getGroupKey() + dict.getGroupCode(), dict.getDictCode(), dict);
            if(!"root".equals(dict.getGroupCode())){
                redisHelper.putMap(getTypeKey() + dict.getTypeCode(), dict.getDictCode(), dict);
            }
        }
        redisHelper.putValue(getDictKey() + dict.getDictCode(), dict);
    }

    /**
     * 获取某个分组字典
     */
    public <T extends Dict> List<T> getGroup(String groupCode) {
        if(StringUtils.isBlank(groupCode)){
            return new ArrayList<>();
        }
        Map<String, T> map = redisHelper.getMap(getGroupKey() + groupCode);
        List<T> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingInt(Dict::getDictOrder));
        return list;
    }

    /**
     * 获取某个类型字典
     */
    public <T extends Dict> List<T> getType(String typeCode) {
        if(StringUtils.isBlank(typeCode)){
            return new ArrayList<>();
        }
        Map<String, T> map = redisHelper.getMap(getTypeKey() + typeCode);
        List<T> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingInt(Dict::getDictOrder));
        return list;
    }

    /**
     * 获取字典
     */
    public <T extends Dict> T getDict(String dictCode) {
        if(StringUtils.isBlank(dictCode)){
            return null;
        }
        return redisHelper.getValue(getDictKey() + dictCode);
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
    public void removeDict(String dictCode) {
        if(StringUtils.isBlank(dictCode)){
            return;
        }
        Dict dict = redisHelper.getValue(getDictKey() + dictCode);
        if(dict == null){
            return;
        }
        redisHelper.delete(getDictKey() + dictCode);
        redisHelper.removeFromMap(getTypeKey() + dict.getTypeCode(), dictCode);
        redisHelper.removeFromMap(getGroupKey() + dict.getGroupCode(), dictCode);
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
    public void removeType(String typeCode) {
        if(StringUtils.isBlank(typeCode)){
            return;
        }
        Map<String, Dict> dictMap = redisHelper.getMap(getTypeKey() + typeCode);
        for (Dict dict : dictMap.values()) {
            redisHelper.delete(getDictKey() + dict.getDictCode());
            redisHelper.removeFromMap(getGroupKey() + dict.getGroupCode(), dict.getDictCode());
        }
        redisHelper.delete(getTypeKey() + typeCode);
        redisHelper.removeFromMap(getGroupKey() + "group", typeCode);
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
    public void removeGroup(String groupCode) {
        if(StringUtils.isBlank(groupCode)){
            return;
        }
        Map<String, Dict> typeMap = redisHelper.getMap(getTypeKey() + groupCode);
        redisHelper.delete(getTypeKey() + groupCode);
        for (Dict type : typeMap.values()) {
            redisHelper.removeFromMap(getGroupKey() + "group", type.getDictCode());
        }

        Map<String, Dict> dictMap = redisHelper.getMap(getGroupKey() + groupCode);
        for (Dict dict : dictMap.values()) {
            redisHelper.delete(getDictKey() + dict.getDictCode());
        }
        redisHelper.delete(getGroupKey() + groupCode);
        redisHelper.removeFromMap(getGroupKey() + "root", groupCode);
    }
}
