package com.common.collect.container.excel.pojo;

import lombok.Data;

import java.util.List;

/**
 * Created by hznijianfeng on 2019/5/28.
 */

@Data
public class EventModelParam {

    private boolean sheetStart;

    private int needReadColNum;

    private int curSheetIndex;

    private int sheetAlreadyReadRowNum;

    private List<List<String>> rows;

}
