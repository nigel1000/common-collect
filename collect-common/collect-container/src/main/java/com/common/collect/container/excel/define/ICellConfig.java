package com.common.collect.container.excel.define;


import com.common.collect.api.excps.UnifiedException;
import com.common.collect.container.excel.ExcelSession;
import com.common.collect.container.excel.base.ExcelConstants;
import com.common.collect.container.excel.context.ExcelContext;
import lombok.Data;
import org.apache.poi.ss.usermodel.CellStyle;

import java.io.Serializable;

/**
 * Created by nijianfeng on 2018/8/26.
 */
public interface ICellConfig {

    @Data
    class ExcelCellConfigInfo implements Serializable {

        // 列宽
        private Integer colWidth;

        // 是否隐藏
        private boolean isHidden = false;

        // 默认风格
        private CellStyle cellStyle;

        public void vailSelf() {
            if (this.getColWidth() != null && this.getColWidth() <= 0) {
                throw UnifiedException.gen(ExcelConstants.MODULE, "列宽不能小于0");
            }
        }

    }

    ExcelCellConfigInfo pullCellConfig(Object value, ExcelSession excelSession, ExcelContext excelContext);

}
