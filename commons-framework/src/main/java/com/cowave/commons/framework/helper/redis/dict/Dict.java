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

/**
 * 字典
 *
 * @author shanhuiming
 */
public interface Dict {

    /**
     * 字典排序
     */
    Integer getDictOrder();

    /**
     * 字典分组
     */
    String getGroupCode();

    /**
     * 字典类型
     */
    String getTypeCode();

    /**
     * 字典码
     */
    String getDictCode();

    /**
     * 字典名称
     */
    String getDictLabel();

    /**
     * 字典值
     */
    Object getDictValue();

    /**
     * 设置字典值
     */
    void setDictValue(Object dictValue);

    /**
     * 值转换器
     */
    String getValueParser();

    /**
     * 值转换器参数
     */
    String getValueParam();
}
