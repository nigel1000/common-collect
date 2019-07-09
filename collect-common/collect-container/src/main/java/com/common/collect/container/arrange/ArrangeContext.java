package com.common.collect.container.arrange;

import com.common.collect.api.excps.UnifiedException;
import com.common.collect.container.JsonUtil;
import com.common.collect.container.arrange.context.BizContext;
import com.common.collect.container.arrange.context.BizFunctionChain;
import com.common.collect.container.arrange.context.ConfigContext;
import com.common.collect.container.arrange.enums.FunctionMethodOutFromEnum;
import com.common.collect.container.arrange.enums.FunctionMethodTypeEnum;
import com.common.collect.util.ClassUtil;
import com.common.collect.util.EmptyUtil;
import com.common.collect.util.SplitUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nijianfeng on 2019/7/6.
 */

@Data
@Slf4j
public class ArrangeContext {


    private ArrangeContext() {
    }

    public static ArrangeRetContext runBiz(String bizKey, String paramJson) {
        BizContext bizContext = BizContext.getBizContextByKey(bizKey);
        ArrangeRetContext retContext = new ArrangeRetContext();
        retContext.setBizKey(bizContext.getBizKey());
        List<BizFunctionChain> bizFunctionChains = bizContext.getBizFunctionChains();
        int size = bizFunctionChains.size();
        Object ret = null;
        Object arg = null;
        for (int i = 0; i < size; i++) {
            BizFunctionChain bizFunctionChain = bizFunctionChains.get(i);
            if (bizFunctionChain.getFunctionMethodTypeEnum().equals(FunctionMethodTypeEnum.inputLessEqualOne)) {
                Class<?> paramType = bizFunctionChain.getParamTypes()[0];
                if (i == 0) {
                    if (paramJson != null) {
                        arg = JsonUtil.json2bean(paramJson, paramType);
                    }
                } else {
                    Map<String, Object> paramMap = new HashMap<>();
                    Map<String, String> inOutMap = bizFunctionChain.getInOutMap();
                    for (Map.Entry<String, String> entry : inOutMap.entrySet()) {
                        String outField = entry.getKey();
                        String inField = entry.getValue();
                        FunctionMethodOutFromEnum outFrom = bizFunctionChain.getFunctionMethodOutFromEnum();
                        if (outFrom.equals(FunctionMethodOutFromEnum.output)) {
                            if (ret != null) {
                                paramMap.put(inField, ClassUtil.getFieldValue(ret, outField));
                            }
                        } else if (outFrom.equals(FunctionMethodOutFromEnum.input)) {
                            if (arg != null) {
                                paramMap.put(inField, ClassUtil.getFieldValue(arg, outField));
                            }
                        }
                    }
                    if (EmptyUtil.isNotEmpty(paramMap)) {
                        arg = JsonUtil.json2bean(JsonUtil.bean2json(paramMap), paramType);
                    } else {
                        arg = null;
                    }
                }
                ret = ClassUtil.invoke(bizFunctionChain.getTarget(), bizFunctionChain.getMethod(), arg);
                if (bizFunctionChain.getFunctionInKeep()) {
                    retContext.putInputMap(SplitUtil.join(bizFunctionChain.getBizKeyRoute(), "-") + "-" + bizFunctionChain.getFunctionKey() + "-" + i, arg == null ? "null" : arg);
                }
                if (bizFunctionChain.getFunctionOutKeep()) {
                    retContext.putOutputMap(SplitUtil.join(bizFunctionChain.getBizKeyRoute(), "-") + "-" + bizFunctionChain.getFunctionKey() + "-" + i, ret == null ? "null" : ret);
                }
            } else {
                throw UnifiedException.gen(bizFunctionChain.getTarget().getClass().getName() + "#" + bizFunctionChain.getMethod().getName() + " 入参只能是一个");
            }
        }
        retContext.setLastRet(ret);
        retContext.setLastArg(arg);
        return retContext;
    }

    public synchronized static void load(Object... obj) {
        ConfigContext.load(obj);
    }

}
