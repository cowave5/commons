/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
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
public class AlarmStatusConverter implements Converter<Integer> {

	public WriteCellData<String> convertToExcelData(WriteConverterContext<Integer> context) {
		String value = ""; 
		Integer status = context.getValue();
		if(status != null) {
		    if(status == 0) {
                value = "未处理"; 
            }else if(status == 1) {
				value = "确认"; 
			}else if(status == 2) {
				value = "误报";
			}
		}
		return new WriteCellData<>(value);
	}
}
