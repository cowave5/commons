/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.ids;

import cn.hutool.core.lang.Assert;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author shanhuiming
 *
 */
public class IdGenerator {

    private final AtomicInteger index = new AtomicInteger(0);

    /**
     * 获取固定长度的id
     * @param prefix     id前缀
     * @param suffix     id后缀
     * @param dateFormat 时间格式
     * @param indexLimit index上限
     */
    public String generateIdWithDate(String prefix, String suffix, String dateFormat, int indexLimit){
        Assert.isTrue(StringUtils.isNotBlank(dateFormat), "dateFormat cannot be empty.");
        Assert.isTrue(indexLimit > 0, "indexLimit must greater than 0.");

        String date = new SimpleDateFormat(dateFormat).format(System.currentTimeMillis());
        int limitLen = String.valueOf(indexLimit).length();

        StringBuilder builder = new StringBuilder();
        if(StringUtils.isNotBlank(prefix)){
            builder.append(prefix);
        }

        builder.append(date);

        if(StringUtils.isNotBlank(suffix)){
            builder.append(suffix);
        }

        String currentIndex = String.valueOf(index.incrementAndGet() % indexLimit);
        int currentLen = currentIndex.length();

        int currentLimit = 1;
        for(int i = 1; i < currentLen; i++){
            currentLimit *= 10;
        }
        for(int i = currentIndex.length(); i < limitLen; i++){
            currentLimit *= 10;
            if(currentLimit < indexLimit){
                builder.append('0');
            }
        }
        return builder.append(currentIndex).toString();
    }
}
