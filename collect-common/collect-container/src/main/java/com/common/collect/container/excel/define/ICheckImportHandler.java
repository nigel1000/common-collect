package com.common.collect.container.excel.define;


import com.common.collect.container.excel.context.ExcelContext;

/**
 * Created by nijianfeng on 2018/8/26.
 */
public interface ICheckImportHandler {

    void check(Object value, String fieldName, ExcelContext excelContext);

}
