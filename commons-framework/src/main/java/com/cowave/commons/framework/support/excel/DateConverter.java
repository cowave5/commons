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

import java.text.SimpleDateFormat;
import java.util.Date;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.converters.WriteConverterContext;
import com.alibaba.excel.metadata.data.WriteCellData;

/**
 * 
 * @author shanhuiming
 * 
 */
public class DateConverter implements Converter<Date> {
    
    private static final String FORMAT = "yyyy-MM-dd HH:mm:ss"; 

    public WriteCellData<String> convertToExcelData(WriteConverterContext<Date> context) {
        String value = ""; 
        Date date = context.getValue();
        if(date != null) {
            value = new SimpleDateFormat(FORMAT).format(date);
        }
        return new WriteCellData<>(value);
    }
}
