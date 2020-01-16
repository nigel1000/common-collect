package com.common.collect.container.idoc.context;

import com.common.collect.api.idoc.IDocMethod;
import com.common.collect.container.idoc.base.GlobalConfig;
import com.common.collect.util.EmptyUtil;
import com.common.collect.util.StringUtil;
import lombok.Data;
import lombok.NonNull;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by nijianfeng on 2020/1/11.
 */
@Data
public class IDocMethodContext implements Serializable {

    private String className;
    private String methodName;

    private String id;
    private String name;
    private String author;
    private String requestUrl;
    private String requestMethod;
    private boolean reCreate = true;

    private Map<String, IDocFieldObj> request = new LinkedHashMap<>();
    private Map<String, IDocFieldObj> response = new LinkedHashMap<>();

    public static IDocMethodContext of(@NonNull IDocMethod iDocMethod, RequestMapping requestMapping) {
        IDocMethodContext context = new IDocMethodContext();
        context.setId(iDocMethod.id());
        context.setName(iDocMethod.name());
        context.setAuthor(iDocMethod.author());
        context.setRequestUrl(iDocMethod.requestUrl());
        context.setRequestMethod(iDocMethod.requestMethod());
        if (requestMapping != null) {
            context.setRequestUrl(StringUtil.join(requestMapping.value(), ","));
            context.setRequestMethod(StringUtil.join(requestMapping.method(), ","));
        }
        // 赋值 reCreate
        if (GlobalConfig.reCreate != null) {
            context.setReCreate(GlobalConfig.reCreate);
        } else {
            context.setReCreate(iDocMethod.reCreate());
        }

        return context;
    }

    public IDocMethodContext addRequest(@NonNull IDocFieldObj value) {
        request.put(value.getName(), value);
        return this;
    }

    public IDocMethodContext addRequest(Map<String, IDocFieldObj> value) {
        if (EmptyUtil.isEmpty(value)) {
            return this;
        }
        request.putAll(value);
        return this;
    }

    public IDocMethodContext addResponse(Map<String, IDocFieldObj> response) {
        if (EmptyUtil.isEmpty(response)) {
            return this;
        }
        this.response.putAll(response);
        return this;
    }

    public void sortMap(Map<String, IDocFieldObj> map) {
        if (EmptyUtil.isEmpty(map)) {
            return;
        }
        Map<String, IDocFieldObj> baseMap = new LinkedHashMap<>();

        Map<String, IDocFieldObj> objStringMap = new LinkedHashMap<>();
        Map<String, IDocFieldObj> objMap = new LinkedHashMap<>();

        Map<String, IDocFieldObj> arrayBaseMap = new LinkedHashMap<>();
        Map<String, IDocFieldObj> arrayObjStringMap = new LinkedHashMap<>();
        Map<String, IDocFieldObj> arrayObjMap = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            if (v.getValue() instanceof Map) {
                sortMap((Map<String, IDocFieldObj>) v.getValue());
            }
            if (v.isObjectType()) {
                if (v.getValue() instanceof String) {
                    objStringMap.put(k, v);
                } else if (v.getValue() instanceof Map) {
                    objMap.put(k, v);
                }
            } else if (v.isArrayType()) {
                if (v.isArrayObjectType()) {
                    if (v.getValue() instanceof String) {
                        arrayObjStringMap.put(k, v);
                    } else if (v.getValue() instanceof Map) {
                        arrayObjMap.put(k, v);
                    }
                } else {
                    arrayBaseMap.put(k, v);
                }
            } else {
                baseMap.put(k, v);
            }
        });
        map.clear();
        map.putAll(baseMap);
        map.putAll(objStringMap);
        map.putAll(objMap);
        map.putAll(arrayObjStringMap);
        map.putAll(arrayBaseMap);
        map.putAll(arrayObjMap);
    }


}
