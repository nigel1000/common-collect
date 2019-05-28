package com.common.collect.container.excel;

import com.common.collect.api.excps.UnifiedException;
import com.common.collect.container.excel.annotations.model.ExcelExportModel;
import com.common.collect.container.excel.base.ExcelConstants;
import com.common.collect.container.excel.define.ICellConfig;
import com.common.collect.container.excel.define.IConvertExportHandler;
import com.common.collect.container.excel.pojo.ExcelExportParam;
import com.common.collect.util.EmptyUtil;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.common.collect.container.excel.ExcelExportUtil.ExcelType.BIG_XLSX;

/**
 * Created by nijianfeng on 2018/8/26.
 */
public class ExcelExportUtil extends ExcelSession {

    @Setter
    @Getter
    private boolean isCalRowHeight = false;
    @Getter
    private ExcelType excelType;

    private Map<String, ExcelExportParam> excelExportParamMap = Maps.newHashMap();

    @SuppressWarnings("unchecked")
    public <C> ExcelExportParam<C> getExcelExportParam(Class<C> type) {
        String key = type.getTypeName();
        if (excelExportParamMap.containsKey(key)) {
            return excelExportParamMap.get(key);
        }
        ExcelExportParam<C> excelExportParam = new ExcelExportParam<>(type);
        excelExportParamMap.putIfAbsent(key, excelExportParam);
        return excelExportParam;
    }

    // BIG_XLSX 由于其实现方式 不能回写
    public enum ExcelType {
        BIG_XLSX("xlsx"), XLSX("xlsx"), XLS("xls"),

        ;
        @Getter
        private String type;

        ExcelType(String type) {
            this.type = type;
        }

    }

    @Override
    // 模版文件不能使用 BIG_XLSX 写，由于xlsx已有数据，而获取已有数据行时却为空
    public Row getRow(int rowIndex) {
        try {
            Row row = this.getSheet().getRow(rowIndex);
            if (row == null) {
                row = this.getSheet().createRow(rowIndex);
            }
            return row;
        } catch (Exception ex) {
            if (BIG_XLSX == getExcelType()) {
                // https://blog.csdn.net/qq_31615049/article/details/82228812
                Sheet sheet = ((SXSSFWorkbook) getWorkbook()).getXSSFWorkbook().getSheetAt(getActiveSheetIndex());
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    row = sheet.createRow(rowIndex);
                }
                return row;
            } else {
                throw ex;
            }
        }
    }

    @Override
    public int getLastRowNum() {
        if (BIG_XLSX == getExcelType()) {
            Sheet sheet = ((SXSSFWorkbook) getWorkbook()).getXSSFWorkbook().getSheetAt(getActiveSheetIndex());
            // 已存在模板的最大行数和写入的最大行数 取最大的
            return Math.max(sheet.getLastRowNum(), this.getSheet().getLastRowNum());
        } else {
            return super.getLastRowNum();
        }
    }

    // 新建文件导出
    public ExcelExportUtil(@NonNull String sheetName, @NonNull ExcelType excelType) {
        if (excelType == BIG_XLSX) {
            setWorkbook(new SXSSFWorkbook());
        } else if (ExcelType.XLSX.equals(excelType)) {
            setWorkbook(new XSSFWorkbook());
        } else if (ExcelType.XLS.equals(excelType)) {
            setWorkbook(new HSSFWorkbook());
        } else {
            throw UnifiedException.gen(ExcelConstants.MODULE, "没有此类型的excel");
        }
        setSheet(createSheet(sheetName));
        this.excelType = excelType;
    }

    // 从已有模板中导出
    public ExcelExportUtil(@NonNull ExcelType excelType, @NonNull InputStream inputStream) {
        try {
            if (excelType == BIG_XLSX) {
                setWorkbook(new SXSSFWorkbook(new XSSFWorkbook(inputStream)));
            } else {
                setWorkbook(WorkbookFactory.create(inputStream));
            }
            changeSheet(0);
            this.excelType = excelType;
        } catch (Exception e) {
            throw UnifiedException.gen(ExcelConstants.MODULE, "excel 文件流有误!", e);
        }
    }

    public ExcelExportUtil(@NonNull ExcelType excelType, @NonNull String filePath) {
        try {
            if (excelType == BIG_XLSX) {
                setWorkbook(new SXSSFWorkbook(new XSSFWorkbook(filePath)));
            } else {
                setWorkbook(WorkbookFactory.create(new FileInputStream(filePath)));
            }
            changeSheet(0);
            this.excelType = excelType;
        } catch (Exception e) {
            throw UnifiedException.gen(ExcelConstants.MODULE, filePath + " 文件流有误!", e);
        }
    }

    public ExcelExportUtil(@NonNull ExcelType excelType, @NonNull Workbook workbook, Sheet sheet) {
        if (excelType == BIG_XLSX) {
            setWorkbook(new SXSSFWorkbook((XSSFWorkbook) workbook));
            setSheet(getWorkbook().getSheet(sheet.getSheetName()));
        } else {
            setWorkbook(workbook);
            setSheet(sheet);
        }
        this.excelType = excelType;
    }

    public ExcelExportUtil(@NonNull ExcelType excelType, @NonNull Workbook workbook) {
        if (excelType == BIG_XLSX) {
            setWorkbook(new SXSSFWorkbook((XSSFWorkbook) workbook));
        } else {
            setWorkbook(workbook);
        }
        changeSheet(0);
        this.excelType = excelType;
    }

    // 导出 一行数据 到 excel
    public void exportData(List<?> data, List<Integer> colIndex, int rowIndex) {
        if (EmptyUtil.isEmpty(data) || EmptyUtil.isEmpty(colIndex) || data.size() != colIndex.size()) {
            throw UnifiedException.gen(ExcelConstants.MODULE, "data和colIndex 必须不为空并且size相等");
        }
        // 组装数据
        Row row = this.getRow(rowIndex);
        for (int i = 0; i < data.size(); i++) {
            setCellValue(row.getRowNum(), colIndex.get(i), data.get(i));
        }
    }

    public void exportData(List<?> data, int rowIndex) {
        if (CollectionUtils.isEmpty(data)) {
            throw UnifiedException.gen(ExcelConstants.MODULE, "data 必须不为空");
        }
        // 组装数据
        Row row = this.getRow(rowIndex);
        for (int i = 0; i < data.size(); i++) {
            setCellValue(row.getRowNum(), i, data.get(i));
        }
    }

    // 导出 title 到 excel 的 第一行
    public <C> void exportTitle(Class<C> type) {
        exportTitle(type, 0);
    }

    // 导出 title 到 excel 的 指定行
    public <C> void exportTitle(Class<C> type, int rowIndex) {
        // 组装数据
        ExcelExportParam<C> excelExportParam = getExcelExportParam(type);
        // 设置标题列
        int maxColIndex = -1;
        for (Map.Entry<String, ExcelExportParam.ExportInfo<C>> entry : excelExportParam.getFieldExportMap()
                .entrySet()) {
            ExcelExportModel excelExportModel = entry.getValue().getExcelExportModel();
            int colIndex = excelExportModel.getColIndex();
            if (maxColIndex < colIndex) {
                maxColIndex = colIndex;
            }
            String title = excelExportModel.getTitle();
            setCellValue(rowIndex, colIndex, title);
        }
        // 自适应行高
        if (isCalRowHeight && maxColIndex != -1) {
            rowHeightAutoFit(new CellRangeAddress(rowIndex, rowIndex, 0, maxColIndex));
        }
    }

    // 通过 模型 导出 数据 到excel
    public <C> void export(@NonNull List<C> data, Class<C> type) {
        // 导出 title
        exportTitle(type);
        // 导出数据
        export(data, type, 1);
    }

    // 只导数据不导出标题
    public <C> void exportForward(@NonNull List<C> data, Class<C> type) {
        // 导出数据
        int startRowIndex = this.getLastRowNum();
        if (startRowIndex != 0 || !this.isEmptyRow(0)) {
            startRowIndex++;
        }
        export(data, type, startRowIndex);
    }

    public <C> void export(@NonNull List<C> data, Class<C> type, int startRowIndex) {
        ExcelExportParam<C> excelExportParam = getExcelExportParam(type);
        exportData(data, excelExportParam, startRowIndex);
    }

    private <C> void exportData(@NonNull List<C> data, ExcelExportParam<C> excelExportParam, int rowOffset) {
        for (C obj : data) {
            Row row = this.getRow(rowOffset);
            int maxColIndex = -1;
            for (Map.Entry<String, ExcelExportParam.ExportInfo<C>> entry : excelExportParam.getFieldExportMap()
                    .entrySet()) {
                ExcelExportParam.ExportInfo exportInfo = entry.getValue();
                int colIndex = exportInfo.getExcelExportModel().getColIndex();
                if (maxColIndex < colIndex) {
                    maxColIndex = colIndex;
                }
                // 利用反射赋值
                Object result = excelExportParam.getFieldValue(entry.getKey(), obj);
                for (IConvertExportHandler convertHandler : exportInfo.getExcelConvertModel()
                        .getConvertExportHandlersList()) {
                    Object convert = convertHandler.convert(result, exportInfo);
                    if (convert != null) {
                        result = convert;
                    }
                }
                setCellValue(row.getRowNum(), colIndex, result);
                ICellConfig cellConfig = exportInfo.getExcelExportModel().getCellConfigImpl();
                if (cellConfig != null) {
                    ICellConfig.ExcelCellConfigInfo cellConfigInfo =
                            cellConfig.pullCellConfig(result, this, excelExportParam);
                    if (cellConfigInfo != null) {
                        setCellStyle(row.getRowNum(), colIndex, cellConfigInfo.getCellStyle());
                        if (cellConfigInfo.isHidden()) {
                            setHiddenColumn(colIndex, cellConfigInfo.isHidden());
                        }
                        if (cellConfigInfo.getColWidth() != null) {
                            setCellWidth(colIndex, cellConfigInfo.getColWidth());
                        }
                    }
                }
            }
            // 自适应行高
            if (isCalRowHeight && maxColIndex != -1) {
                rowHeightAutoFit(new CellRangeAddress(rowOffset, rowOffset, 0, maxColIndex));
            }
        }
    }

    public File saveTemp(String prefix) {
        return super.saveTemp(prefix, "." + this.getExcelType().getType());
    }
}
