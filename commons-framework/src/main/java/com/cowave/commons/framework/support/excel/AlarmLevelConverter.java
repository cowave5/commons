/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.excel;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.converters.WriteConverterContext;
import com.alibaba.excel.metadata.data.WriteCellData;

/**
 * 
 * @author shanhuiming
 * 
 */
public class AlarmLevelConverter implements Converter<Integer> {

    // 1:提示 2:普通 3:重要 4:严重'
    public WriteCellData<String> convertToExcelData(WriteConverterContext<Integer> context) {
        String value = "提示";  
        Integer status = context.getValue();
        if(status != null) {
            if(status == 2) {
                value = "普通"; 
            }else if(status == 3) {
                value = "重要";
            }else if(status == 4) {
                value = "严重";
            }
        }
        return new WriteCellData<>(value);
    }
}
