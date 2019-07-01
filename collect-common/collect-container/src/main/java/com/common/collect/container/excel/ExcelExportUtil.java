package com.common.collect.container.excel;

import com.common.collect.api.excps.UnifiedException;
import com.common.collect.container.excel.base.ExcelConstants;
import com.common.collect.container.excel.client.PoiClient;
import com.common.collect.container.excel.context.ExcelContext;
import com.common.collect.container.excel.define.ICellConfig;
import com.common.collect.container.excel.define.IConvertExportHandler;
import com.common.collect.util.EmptyUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
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
import java.util.concurrent.ConcurrentHashMap;

import static com.common.collect.container.excel.ExcelExportUtil.ExcelType.BIG_XLSX;

/**
 * Created by nijianfeng on 2018/8/26.
 */
@Slf4j
public class ExcelExportUtil extends ExcelSession {

    @Setter
    @Getter
    private boolean isCalRowHeight = false;
    @Getter
    private ExcelType excelType;

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
            return super.getRow(rowIndex);
        } catch (Exception ex) {
            if (BIG_XLSX == getExcelType()) {
                // https://blog.csdn.net/qq_31615049/article/details/82228812
                Sheet sheet = ((SXSSFWorkbook) getWorkbook()).getXSSFWorkbook().getSheetAt(getActiveSheetIndex());
                return PoiClient.getRow(sheet, rowIndex);
            } else {
                throw ex;
            }
        }
    }

    private Map<Integer, Integer> existRowNumMap = new ConcurrentHashMap<>();

    private Integer getExistRowNum() {
        return existRowNumMap.getOrDefault(getSheetIndex(), -1);
    }

    private Integer setExistRowNum(int num) {
        return existRowNumMap.put(getSheetIndex(), num);
    }

    @Override
    public void changeSheet(String sheetName) {
        super.changeSheet(sheetName);
        initExistRowNum();
    }

    @Override
    public void changeSheet(int sheetIndex) {
        super.changeSheet(sheetIndex);
        initExistRowNum();
    }

    private void initExistRowNum() {
        if (excelType == BIG_XLSX) {
            if (getExistRowNum() == -1) {
                Sheet sheet = ((SXSSFWorkbook) getWorkbook()).getXSSFWorkbook().getSheetAt(getActiveSheetIndex());
                // 已存在模板的最大行数
                int existRowNum = PoiClient.getLastRowNum(sheet);
                setExistRowNum(existRowNum);
            }
            log.info("sheetIndex:{}, existRowNum:{}", getSheetIndex(), getExistRowNum());
        }
    }

    @Override
    public int getLastRowNum() {
        if (excelType == BIG_XLSX) {
            return getExistRowNum();
        } else {
            return super.getLastRowNum();
        }
    }

    public int getNextRowNum() {
        int startRowIndex = this.getLastRowNum();
        if (startRowIndex != 0 || !this.isEmptyRow(0)) {
            startRowIndex++;
        }
        return startRowIndex;
    }

    @Override
    public Cell setCellValue(int rowIndex, int colIndex, Object value) {
        Cell cell = super.setCellValue(rowIndex, colIndex, value);
        if (excelType == BIG_XLSX) {
            if (getExistRowNum() < rowIndex) {
                setExistRowNum(rowIndex);
            }
        }
        return cell;
    }

    // 新建文件导出
    public ExcelExportUtil(@NonNull String sheetName, @NonNull ExcelType excelType) {
        if (excelType == BIG_XLSX) {
            setWorkbook(new SXSSFWorkbook(50));
        } else if (ExcelType.XLSX.equals(excelType)) {
            setWorkbook(new XSSFWorkbook());
        } else if (ExcelType.XLS.equals(excelType)) {
            setWorkbook(new HSSFWorkbook());
        } else {
            throw UnifiedException.gen(ExcelConstants.MODULE, "没有此类型的excel");
        }
        this.excelType = excelType;
        changeSheet(sheetName);
    }

    // 从已有模板中导出
    public ExcelExportUtil(@NonNull ExcelType excelType, @NonNull InputStream inputStream) {
        try {
            if (excelType == BIG_XLSX) {
                setWorkbook(new SXSSFWorkbook(new XSSFWorkbook(inputStream), 50));
            } else {
                setWorkbook(WorkbookFactory.create(inputStream));
            }
            this.excelType = excelType;
            changeSheet(0);
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
            this.excelType = excelType;
            changeSheet(0);
        } catch (Exception e) {
            throw UnifiedException.gen(ExcelConstants.MODULE, filePath + " 文件流有误!", e);
        }
    }

    public ExcelExportUtil(@NonNull ExcelType excelType, @NonNull Workbook workbook, Sheet sheet) {
        if (excelType == BIG_XLSX) {
            setWorkbook(new SXSSFWorkbook((XSSFWorkbook) workbook));
        } else {
            setWorkbook(workbook);
        }
        this.excelType = excelType;
        changeSheet(sheet.getSheetName());
    }

    public ExcelExportUtil(@NonNull ExcelType excelType, @NonNull Workbook workbook) {
        if (excelType == BIG_XLSX) {
            setWorkbook(new SXSSFWorkbook((XSSFWorkbook) workbook));
        } else {
            setWorkbook(workbook);
        }
        this.excelType = excelType;
        changeSheet(0);
    }

    // 导出 一行数据 到 excel
    public void exportData(List<?> oneRow, List<Integer> colIndex, int rowIndex) {
        if (EmptyUtil.isEmpty(oneRow) || EmptyUtil.isEmpty(colIndex) || oneRow.size() != colIndex.size()) {
            throw UnifiedException.gen(ExcelConstants.MODULE, "oneRow 和 colIndex 必须不为空并且size相等");
        }
        // 组装数据
        Row row = this.getRow(rowIndex);
        for (int i = 0; i < oneRow.size(); i++) {
            setCellValue(row.getRowNum(), colIndex.get(i), oneRow.get(i));
        }
    }

    public void exportData(List<?> oneRow, int rowIndex) {
        if (EmptyUtil.isEmpty(oneRow)) {
            throw UnifiedException.gen(ExcelConstants.MODULE, "oneRow 必须不为空");
        }
        // 组装数据
        Row row = this.getRow(rowIndex);
        for (int i = 0; i < oneRow.size(); i++) {
            setCellValue(row.getRowNum(), i, oneRow.get(i));
        }
    }

    public void exportData(List<List<?>> rows) {
        if (EmptyUtil.isEmpty(rows)) {
            throw UnifiedException.gen(ExcelConstants.MODULE, "rows 必须不为空");
        }
        // 组装数据
        for (List<?> oneRow : rows) {
            Row row = this.getRow(this.getNextRowNum());
            for (int i = 0; i < oneRow.size(); i++) {
                setCellValue(row.getRowNum(), i, oneRow.get(i));
            }
        }
    }

    // 导出 title 到 excel 的 第一行
    public <C> void exportTitle(Class<C> type) {
        exportTitle(type, 0);
    }

    // 导出 title 到 excel 的 指定行
    public <C> void exportTitle(Class<C> type, int rowIndex) {
        // 组装数据
        ExcelContext excelContext = ExcelContext.excelContext(type);
        // 设置标题列
        int maxColIndex = -1;
        for (String fieldName : excelContext.getFieldNameList()) {
            if (!excelContext.isExport(fieldName)) {
                continue;
            }
            int colIndex = excelContext.getExcelExportColIndexMap().get(fieldName);
            if (maxColIndex < colIndex) {
                maxColIndex = colIndex;
            }
            String title = excelContext.getExcelExportTitleMap().get(fieldName);
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
        int startRowIndex = this.getNextRowNum();
        export(data, type, startRowIndex);
    }

    public <C> void export(@NonNull List<C> data, Class<C> type, int startRowIndex) {
        ExcelContext excelContext = ExcelContext.excelContext(type);
        exportData(data, excelContext, startRowIndex);
    }

    private <C> void exportData(@NonNull List<C> data, ExcelContext excelContext, int rowOffset) {
        for (C obj : data) {
            if (obj == null) {
                continue;
            }
            int maxColIndex = -1;
            Row row = null;
            for (String fieldName : excelContext.getFieldNameList()) {
                if (!excelContext.isExport(fieldName)) {
                    continue;
                }
                if (row == null) {
                    row = this.getRow(rowOffset);
                }
                int colIndex = excelContext.getExcelExportColIndexMap().get(fieldName);
                if (maxColIndex < colIndex) {
                    maxColIndex = colIndex;
                }
                // 利用反射赋值
                Object result = excelContext.getFieldValue(fieldName, obj);
                for (IConvertExportHandler convertHandler : excelContext.getExcelConvertExportHandlerMap()
                        .get(fieldName)) {
                    Object convert = convertHandler.convert(result, fieldName, excelContext);
                    if (convert != null) {
                        result = convert;
                    }
                }
                setCellValue(row.getRowNum(), colIndex, result);
                ICellConfig cellConfig = excelContext.getExcelExportCellConfigMap().get(fieldName);
                if (cellConfig != null) {
                    ICellConfig.ExcelCellConfigInfo cellConfigInfo =
                            cellConfig.pullCellConfig(result, this, excelContext);
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
            rowOffset++;
        }
    }

    public File saveTemp(String prefix) {
        return super.saveTemp(prefix, "." + this.getExcelType().getType());
    }
}
