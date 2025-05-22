/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.excel.valid;

import cn.hutool.extra.validation.BeanValidationResult;
import cn.hutool.extra.validation.ValidationUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.cowave.commons.client.http.asserts.HttpHintException;
import com.cowave.commons.client.http.asserts.I18Messages;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.cowave.commons.client.http.constants.HttpCode.BAD_REQUEST;

/**
 * 触发@ExcelProperty标记的属性上的Valid校验
 *
 * @author shanhuiming
 */
public class ExcelImportValidListener<T> extends AnalysisEventListener<T> {
    private final List<T> list = new ArrayList<>();
    private final ExcelDataImporter<T> dataImporter;
    private final boolean overwrite;

    public ExcelImportValidListener(ExcelDataImporter<T> dataImporter, boolean overwrite){
        this.dataImporter = dataImporter;
        this.overwrite = overwrite;
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        valid(data, context);
        list.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        dataImporter.importExcelData(list, overwrite);
    }

    private void valid(Object bean, AnalysisContext context) {
        StringBuilder failedBuilder = new StringBuilder(I18Messages.msg("frame.import.failed.msg"));
        Field[] fields = bean.getClass().getDeclaredFields();
        boolean validFailed = false;
        for (Field field : fields) {
            field.setAccessible(true);
            ExcelProperty excel = field.getAnnotation(ExcelProperty.class);
            if(excel == null){
                continue;
            }
            BeanValidationResult validationResult = ValidationUtil.warpValidateProperty(bean, field.getName());
            if (!validationResult.isSuccess()) {
                validFailed = true;
                failedBuilder.append("<br/>").append(getMessage(context.readRowHolder().getRowIndex() + 1, validationResult.getErrorMessages().get(0).getMessage()));
            }
        }
        if(validFailed){
            throw new HttpHintException(BAD_REQUEST, failedBuilder.toString());
        }
    }

    private String getMessage(int rowIndex, String message){
        return I18Messages.msg("frame.excel.failed.row", rowIndex, message);
    }
}
