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
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

/**
 *
 * @author shanhuiming
 *
 */
public class SexConverter implements Converter<Integer> {

    @Override
    public WriteCellData<String> convertToExcelData(WriteConverterContext<Integer> context) {
        Integer status = context.getValue();
        if (status != null) {
            if (status == 0) {
                return new WriteCellData<>("男");
            } else if (status == 1) {
                return new WriteCellData<>("女");
            } else if (status == 2) {
                return new WriteCellData<>("未知");
            }
        }
        return new WriteCellData<>("");
    }

    @Override
    public Integer convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        String stringValue = cellData.getStringValue();
        if ("男".equals(stringValue)) {
            return 0;
        } else if ("女".equals(stringValue)) {
            return 1;
        } else if ("未知".equals(stringValue)) {
            return 2;
        }
        return null;
    }
}
