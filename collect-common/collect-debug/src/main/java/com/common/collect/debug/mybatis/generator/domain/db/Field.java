package com.common.collect.debug.mybatis.generator.domain.db;

import lombok.Data;

/**
 * Created by hznijianfeng on 2019/3/14.
 */

@Data
public class Field {

    private String field;// column名称
    private String type;// 数据库类型
    private String memo;// 注释
    private String numericLength;
    private String numericScale;
    private String isNullable;// 是否可为空
    private Boolean isAutoIncrement;
    private String isDefault;// 默认值
    private String characterLength;

}
