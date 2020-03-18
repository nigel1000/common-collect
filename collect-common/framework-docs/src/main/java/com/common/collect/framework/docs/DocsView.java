package com.common.collect.framework.docs;

import com.common.collect.lib.util.EmptyUtil;
import com.common.collect.lib.util.FunctionUtil;
import com.common.collect.lib.util.fastjson.JsonUtil;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Created by hznijianfeng on 2020/3/18.
 */

public class DocsView {

    public static String htmlView(@NonNull DocsContext.Interface anInterface, @NonNull List<DocsContext.DataType> dataTypes) {
        Map<String, DocsContext.DataType> dataTypeMap = FunctionUtil.keyValueMap(dataTypes, DocsContext.DataType::getName);
        StringBuilder sb = new StringBuilder();
        addHtmlHead(sb);
        addLine("文档名称：" + anInterface.getName() + "<br>", sb);
        addLine("文档描述：" + anInterface.getDescription() + "<br>", sb);
        addLine("文档对应代码：" + anInterface.getClassName() + "<br>", sb);
        addLine("访问地址：" + anInterface.getPath() + "<br>", sb);
        addLine("访问方式：" + anInterface.getMethod() + "<br>", sb);

        addLine("访问入参", sb);
        addLine("<table border=\"1\" width=\"1000\" cellspacing=\"0\" cellpadding=\"5px\">", sb);
        map2Html(anInterface.getParams().getInputs(), dataTypeMap, true, 0, sb);
        addLine("</table>", sb);

//        // mock request
//        addLine("<div>", sb);
//        addLine("<pre>", sb);
//        addLine(JsonUtil.bean2jsonPretty(context.genRequestMock()), sb);
//        addLine("</pre>", sb);
//        addLine("</div>", sb);

        addLine("访问返回", sb);
        addLine("<table border=\"1\" width=\"1000\" cellspacing=\"0\" cellpadding=\"5px\" >", sb);
        map2Html(anInterface.getParams().getOutputs(), dataTypeMap, false, 0, sb);
        addLine("</table>", sb);

        // mock response
//        addLine("<div>", sb);
//        addLine("<pre>", sb);
//        addLine(JsonUtil.bean2jsonPretty(context.genResponseMock()), sb);
//        addLine("</pre>", sb);
//        addLine("</div>", sb);

        addHtmlTail(sb);
        return sb.toString();
    }

    private static void map2Html(List<DocsContext.Parameter> parameters, @NonNull Map<String, DocsContext.DataType> dataTypeMap, boolean isInput, int level, StringBuilder sb) {
        if (EmptyUtil.isEmpty(parameters)) {
            return;
        }
        boolean isFirst = true;
        String temp = "";
        for (int j = 0; j < level; j++) {
            temp = temp.concat("<td> </td>");
        }
        String blank = temp;
        for (DocsContext.Parameter parameter : parameters) {
            if (isFirst) {
                addLine("<tr align=\"left\">", sb);
                String out = blank;
                out += String.format("<td>%s</td>", "名称") +
                        String.format("<td>%s</td>", "类型") +
                        String.format("<td>%s</td>", "Mock值") +
                        String.format("<td>%s</td>", "描述");
                if (isInput) {
                    out += String.format("<td>%s</td>", "是否必填");
                }
                addLine(out, sb);
                addLine("</tr>", sb);
                isFirst = false;
            }
            addLine("<tr align=\"left\">", sb);
            String out = blank;
            // 填充 名称
            out += String.format("<td>%s</td>", parameter.getName());
            // 填充 类型
            if (DocsContext.Parameter.TypeNameEnum.isBaseTypeName(parameter.getTypeName())) {
                if (parameter.isArray()) {
                    out += String.format("<td>%s</td>", "array-" + parameter.getTypeName() + "-" + parameter.getArrayCount());
                } else {
                    out += String.format("<td>%s</td>", parameter.getTypeName());
                }
            } else {
                if (parameter.isArray()) {
                    out += String.format("<td>%s</td>", "array-object-" + parameter.getArrayCount());
                } else {
                    out += String.format("<td>%s</td>", "object");
                }
            }
            // 填充 数据模型默认值
            if (DocsContext.Parameter.TypeNameEnum.isBaseTypeName(parameter.getTypeName())) {
                if (parameter.isArray()) {
                    out += String.format("<td>%s</td>", JsonUtil.bean2json(DocsTool.arrayCountList(parameter.getMockValue(), parameter.getArrayCount())));
                } else {
                    out += String.format("<td>%s</td>", String.valueOf(parameter.getMockValue()));
                }
            } else {
                out += String.format("<td>%s</td>", "");
            }
            // 填充描述
            out += String.format("<td>%s</td>", parameter.getDescription());
            // 填充 是否必填
            if (isInput) {
                out += String.format("<td>%s</td>", parameter.isRequired() + "");
            }

            addLine(out, sb);
            addLine("</tr>", sb);

            if (!DocsContext.Parameter.TypeNameEnum.isBaseTypeName(parameter.getTypeName())) {
                int next = level + 1;
                addLine("<tr align=\"left\">", sb);
                map2Html(dataTypeMap.get(parameter.getTypeName()).getParams(), dataTypeMap, isInput, next, sb);
                addLine("</tr>", sb);
            }
        }
    }

    private static void addLine(String line, @NonNull StringBuilder sb) {
        if (line == null) {
            line = "";
        }
        sb.append(line);
        sb.append("\n");
    }

    private static void addHtmlHead(StringBuilder sb) {
        addLine("<!DOCTYPE html>", sb);
        addLine("<head>", sb);
        addLine("<title>接口定义详细</title>", sb);
        addLine("<meta charset=\"utf-8\">", sb);
        addLine("</head>", sb);
        addLine("<body>", sb);
    }

    private static void addHtmlTail(StringBuilder sb) {
        addLine("</body>", sb);
        addLine("</html>", sb);
    }

}
