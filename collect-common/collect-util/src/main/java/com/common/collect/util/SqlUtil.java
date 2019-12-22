package com.common.collect.util;

import lombok.NonNull;

import java.util.List;

/**
 * Created by hznijianfeng on 2017/2/14.
 */
public class SqlUtil {

    public static String aroundLike(String column) {
        if (EmptyUtil.isBlank(column)) {
            return "";
        }
        return "%" + trim(column) + "%";
    }

    public static String tailLike(String column) {
        if (EmptyUtil.isBlank(column)) {
            return "";
        }
        return trim(column) + "%";
    }

    public static String headLike(String column) {
        if (EmptyUtil.isBlank(column)) {
            return "";
        }
        return "%" + trim(column);
    }

    public static String trim(String column) {
        if (EmptyUtil.isBlank(column)) {
            return "";
        }
        return column.trim();
    }

    public static String paging(String sortBy, Integer limit, Integer offset) {
        String sql = "";
        if (EmptyUtil.isNotBlank(sortBy)) {
            sql += " order by " + sortBy;
        }
        if (limit != null) {
            sql += " limit " + limit;
        }
        if (offset != null) {
            sql += " offset " + offset;
        }
        return sql;
    }

    public static String in(@NonNull String field, @NonNull List<?> values) {
        if (EmptyUtil.isEmpty(values)) {
            return "";
        }
        String sql = "";
        int size = values.size() - 1;
        for (int i = 0; i < size + 1; i++) {
            if (i == 0) {
                sql = field + " in ( ";
            }
            sql += "'" + values.get(i) + "'";
            if (size == i) {
                sql += " ) ";
            } else {
                sql += " , ";
            }
        }
        return sql;
    }

    public static String set(@NonNull String sql, @NonNull List<Object> args, @NonNull String field, Object value) {
        if (value == null) {
            return "";
        }
        if (sql.contains(" set ")) {
            args.add(value);
            return " , " + field + " = ? ";
        } else {
            args.add(value);
            return " set " + field + " = ? ";
        }
    }

}