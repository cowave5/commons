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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
public class CascadeWriteHandler implements SheetWriteHandler {

    private final String parentCellHead;

    private final int parentCellIndex;

    private final int childCellIndex;

    private final int rowSize;

    private final LinkedHashMap<String, List<String>> siteMap;

    public CascadeWriteHandler(LinkedHashMap<String, List<String>> siteMap, String parentCellHead, int parentCellIndex, int childCellIndex, int rowSize) {
        this.siteMap = siteMap;
        this.rowSize = rowSize;
        this.parentCellHead = parentCellHead;
        this.parentCellIndex = parentCellIndex;
        this.childCellIndex = childCellIndex;
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Workbook book = writeWorkbookHolder.getWorkbook();
        Sheet sheet = writeSheetHolder.getSheet();
        //创建隐藏sheet，用来存放地区信息
        Sheet hideSheet = book.createSheet("site");
        book.setSheetHidden(book.getSheetIndex(hideSheet), true);

        // 数据写入，行开头为父级，后面是子级
        int rowIndex = 0;
        Row proviRow = hideSheet.createRow(rowIndex++);
        proviRow.createCell(0).setCellValue("大类列表");
        int cellIndex = 0;
        for (String parent : siteMap.keySet()) {
            Cell proviCell = proviRow.createCell(++cellIndex);
            proviCell.setCellValue(parent);
        }

        for (Map.Entry<String, List<String>> entry : siteMap.entrySet()) {
            List<String> son = entry.getValue();
            Row row = hideSheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(entry.getKey());
            for (int i = 0; i < son.size(); i++) {
                Cell cell = row.createCell(i + 1);
                cell.setCellValue(son.get(i));
            }
            // 添加名称管理器
            String range = getRange(1, rowIndex, son.size());
            Name name = book.createName();
            name.setNameName(entry.getKey());
            String formula = "site!" + range;
            name.setRefersToFormula(formula);
        }

        // 大类下拉框
        CellRangeAddressList expRangeAddressList = new CellRangeAddressList(1, rowSize, parentCellIndex, parentCellIndex);
        DataValidationHelper dvHelper = sheet.getDataValidationHelper();
        DataValidationConstraint expConstraint = dvHelper.createExplicitListConstraint(siteMap.keySet().toArray(new String[]{}));
        setValidation(sheet, dvHelper, expConstraint, expRangeAddressList);

        // 小类规则下拉框
        for (int i = 2; i < rowSize + 1; i++) {
            CellRangeAddressList rangeAddressList = new CellRangeAddressList(i-1 , i-1, childCellIndex, childCellIndex);
            DataValidationConstraint formula = dvHelper.createFormulaListConstraint("INDIRECT($" + parentCellHead + "$" + i + ")");
            setValidation(sheet, dvHelper, formula, rangeAddressList);
        }
    }

    private void setValidation(Sheet sheet, DataValidationHelper helper, DataValidationConstraint constraint, CellRangeAddressList addressList) {
        DataValidation dataValidation = helper.createValidation(constraint, addressList);
        dataValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        dataValidation.setShowErrorBox(true);
        dataValidation.setSuppressDropDownArrow(true);
        dataValidation.createErrorBox("数据错误", "非法数据");
        sheet.addValidationData(dataValidation);
    }

    public String getRange(int offset, int rowId, int colCount) {
        if(colCount == 0){
            colCount = 1;
        }
        char start = (char) ('A' + offset);
        if (colCount <= 25) {
            char end = (char) (start + colCount - 1);
            return "$" + start + "$" + rowId + ":$" + end + "$" + rowId;
        } else {
            char endPrefix = 'A';
            char endSuffix;
            if ((colCount - 25) / 26 == 0 || colCount == 51) {// 26-51之间，包括边界（仅两次字母表计算）
                if ((colCount - 25) % 26 == 0) {// 边界值
                    endSuffix = (char) ('A' + 25);
                } else {
                    endSuffix = (char) ('A' + (colCount - 25) % 26 - 1);
                }
            } else {// 51以上
                if ((colCount - 25) % 26 == 0) {
                    endSuffix = (char) ('A' + 25);
                    endPrefix = (char) (endPrefix + (colCount - 25) / 26 - 1);
                } else {
                    endSuffix = (char) ('A' + (colCount - 25) % 26 - 1);
                    endPrefix = (char) (endPrefix + (colCount - 25) / 26);
                }
            }
            return "$" + start + "$" + rowId + ":$" + endPrefix + endSuffix + "$" + rowId;
        }
    }
}
