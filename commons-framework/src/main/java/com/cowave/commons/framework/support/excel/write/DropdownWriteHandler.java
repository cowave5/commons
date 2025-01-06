/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.excel.write;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;

import java.util.List;

/**
 *
 * @author shanhuiming
 *
 */
public class DropdownWriteHandler implements SheetWriteHandler {

    private final int cellIndex;

    private final int rowSize;

    private final List<String> list;

    public DropdownWriteHandler(List<String> list, int cellIndex, int rowSize) {
        this.list = list;
        this.rowSize = rowSize;
        this.cellIndex = cellIndex;
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        DataValidationHelper helper = sheet.getDataValidationHelper();
        CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(1, rowSize, cellIndex, cellIndex);
        DataValidationConstraint constraint = helper.createExplicitListConstraint(list.toArray(new String[]{}));

        DataValidation dataValidation = helper.createValidation(constraint, cellRangeAddressList);
        dataValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        dataValidation.setShowErrorBox(true);
        dataValidation.setSuppressDropDownArrow(true);
        dataValidation.createErrorBox("数据错误", "非法数据");
        sheet.addValidationData(dataValidation);
    }
}
