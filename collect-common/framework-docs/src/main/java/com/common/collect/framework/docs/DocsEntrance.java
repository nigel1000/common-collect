package com.common.collect.framework.docs;

import com.common.collect.lib.api.docs.DocsDataType;
import com.common.collect.lib.api.docs.DocsField;
import com.common.collect.lib.api.docs.DocsFieldExclude;
import com.common.collect.lib.api.docs.DocsMethod;
import com.common.collect.lib.util.ClassUtil;
import com.common.collect.lib.util.EmptyUtil;
import com.common.collect.lib.util.StringUtil;
import lombok.Data;
import lombok.NonNull;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hznijianfeng on 2020/3/16.
 */

public class DocsEntrance {

    public static Map<String, DocsContext.DataType> docsDataTypes = new HashMap<>();

    public static DocsContext createDocs(@NonNull Class<?> cls) {
        DocsContext docsContext = new DocsContext();
        RequestMapping clsRequestMapping = cls.getAnnotation(RequestMapping.class);
        for (Method method : ClassUtil.getMethods(cls)) {
            DocsMethod docsMethod = method.getAnnotation(DocsMethod.class);
            RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
            if (docsMethod == null || methodRequestMapping == null) {
                continue;
            }
            ParamContext paramContext = new ParamContext();
            paramContext.setCls(cls);
            paramContext.setReturnType(method.getReturnType());
            paramContext.setReturnGenericTypeMap(ClassUtil.getMethodReturnGenericTypeMap(method));
            paramContext.setMethod(method);
            paramContext.setDocsMethod(docsMethod);
            paramContext.setClsRequestMapping(clsRequestMapping);
            paramContext.setMethodRequestMapping(methodRequestMapping);
            // 处理接口
            paramContext.createDocsInterface();
            // 处理接口入参
            paramContext.createDocsInterfaceParamInput();
            // 处理接口返回
            paramContext.createDocsInterfaceParamOutput();

            docsContext.addDocsInterface(paramContext.getDocsInterface());
        }
        docsContext.addDocsDataType(docsDataTypes.values());
        docsDataTypes.clear();
        return docsContext;
    }

    @Data
    public static class ParamContext {
        private Class<?> cls;
        private Class<?> returnType;
        private Map<String, Type> returnGenericTypeMap = new LinkedHashMap<>();
        private Method method;
        private DocsMethod docsMethod;
        private RequestMapping clsRequestMapping;
        private RequestMapping methodRequestMapping;

        private DocsContext.Interface docsInterface = new DocsContext.Interface();
        private DocsContext.InterfaceParameter docsInterfaceParams = docsInterface.getParams();

        public void createDocsInterface() {
            docsInterface.setName(docsMethod.name());
            docsInterface.setClassName(cls.getName() + "#" + method.getName());
            docsInterface.setDescription(docsMethod.desc());
            String url = "";
            RequestMethod[] method = null;
            if (clsRequestMapping != null) {
                url = url.concat(StringUtil.join(clsRequestMapping.value(), ","));
                method = clsRequestMapping.method();
            }
            if (methodRequestMapping != null) {
                url = url.concat(StringUtil.join(methodRequestMapping.value(), ","));
                method = methodRequestMapping.method();
            }
            docsInterface.setPath(url);
            if (EmptyUtil.isNotEmpty(method)) {
                docsInterface.setMethod(DocsContext.Interface.InterfaceMethodEnum.valueOf(method[0].name()).name());
            }
        }

        public void createDocsInterfaceParamOutput() {
            DocsContext.Parameter docsParameter = DocsContext.Parameter.gen(returnType, null, null, null);
            Class<?> actualCls = richParameterArray(returnType, returnGenericTypeMap, docsParameter);
            DocsContext.DataType dataType = richParameterTypeName(actualCls, docsParameter);
            if (!docsParameter.hasTypeName()) {
                return;
            }
            if (docsParameter.isArray() || DocsContext.Parameter.TypeNameEnum.isBaseTypeName(docsParameter.getTypeName())) {
                docsParameter.setName("defResultName");
                docsInterfaceParams.addOutput(docsParameter);
            } else {
                if (dataType != null) {
                    docsInterfaceParams.addOutput(dataType.getParams());
                }
            }
        }

        public void createDocsInterfaceParamInput() {
            // 解析参数
            Parameter[] parameters = method.getParameters();
            DefaultParameterNameDiscoverer discover = new DefaultParameterNameDiscoverer();
            String[] parameterNames = discover.getParameterNames(method);
            // 没有入参或者入参解析有误
            if (parameterNames == null || parameterNames.length != parameters.length) {
                return;
            }
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Class<?> cls = parameter.getType();
                // 排除掉的属性解析
                // 排除掉不处理的类型
                if (parameter.isAnnotationPresent(DocsFieldExclude.class) || DocsTool.clsInBlackList(cls)) {
                    continue;
                }
                DocsContext.Parameter docsParameter = DocsContext.Parameter.gen(
                        cls,
                        parameter.getAnnotation(RequestParam.class),
                        parameter.getAnnotation(DocsField.class),
                        parameterNames[i]);
                Class<?> actualCls = richParameterArray(cls, ClassUtil.getMethodParameterGenericTypeMap(method, i), docsParameter);
                DocsContext.DataType dataType = richParameterTypeName(actualCls, docsParameter);
                if (!docsParameter.hasTypeName()) {
                    // 如果入参上是不可处理的类，但入参上有DocsField，默认typename为string
                    if (parameter.isAnnotationPresent(DocsField.class)) {
                        docsParameter.setTypeName(DocsContext.Parameter.TypeNameEnum.String.getName());
                        docsInterfaceParams.addInput(docsParameter);
                    }
                    continue;
                }
                if (docsParameter.isArray() || DocsContext.Parameter.TypeNameEnum.isBaseTypeName(docsParameter.getTypeName())) {
                    docsInterfaceParams.addInput(docsParameter);
                } else {
                    if (dataType != null) {
                        docsInterfaceParams.addInput(dataType.getParams());
                    }
                }
            }
        }

        private DocsContext.DataType richParameterTypeName(@NonNull Class<?> actualCls, @NonNull DocsContext.Parameter docsParameter) {
            if (DocsTool.clsInBlackList(actualCls) || DocsTool.isArray(actualCls)) {
                return null;
            }
            String typeName = DocsTool.typeNamedDocsParameter(actualCls);
            if (EmptyUtil.isNotEmpty(typeName)) {
                // 系统基本类型
                docsParameter.setTypeName(typeName);
                docsParameter.setMockValue(DocsTool.mockParameterValue(actualCls, docsParameter.getDefaultValue()));
                return null;
            }
            // 对象类型
            DocsContext.DataType dataType = createDocsDataType(actualCls);
            if (dataType != null) {
                docsParameter.setTypeName(dataType.getName());
            }
            return dataType;
        }

        public DocsContext.DataType createDocsDataType(@NonNull Class<?> actualCls) {
            if (DocsTool.clsInBlackList(actualCls) || DocsTool.isArray(actualCls) || actualCls == Object.class) {
                return null;
            }
            DocsContext.DataType dataType = DocsContext.DataType.gen(actualCls, actualCls.getAnnotation(DocsDataType.class));
            if (docsDataTypes.get(dataType.getName()) != null) {
                return docsDataTypes.get(dataType.getName());
            }
            Field[] fields = ClassUtil.getFields(actualCls);
            for (Field field : fields) {
                Class<?> fieldCls = field.getType();
                if (DocsTool.clsInBlackList(fieldCls)) {
                    continue;
                }
                Map<String, Type> fieldGenericTypeMap = new HashMap<>(returnGenericTypeMap);
                // 如果是泛型 或者 list
                Type type = null;
                if (List.class == fieldCls) {
                    type = field.getGenericType();
                }
                if (Object.class == fieldCls) {
                    type = returnGenericTypeMap.get(field.getGenericType().getTypeName());
                }
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    fieldCls = (Class) parameterizedType.getRawType();
                    fieldGenericTypeMap.putAll(ClassUtil.getParameterizedTypeMap(parameterizedType));
                }
                if (type instanceof Class) {
                    fieldCls = (Class) type;
                }
                if (Modifier.isStatic(field.getModifiers())
                        || field.isAnnotationPresent(DocsFieldExclude.class)
                        || DocsTool.clsInBlackList(fieldCls)
                ) {
                    continue;
                }
                DocsContext.Parameter docsParameter = DocsContext.Parameter.gen(
                        fieldCls,
                        null,
                        field.getAnnotation(DocsField.class),
                        field.getName());
                Class<?> fieldActualCls = richParameterArray(fieldCls, fieldGenericTypeMap, docsParameter);
                richParameterTypeName(fieldActualCls, docsParameter);
                if (docsParameter.hasTypeName()) {
                    dataType.addDocsParameter(docsParameter);
                } else if (field.isAnnotationPresent(DocsField.class)) {
                    // 如果属性上是不可处理的类，但属性上有DocsField，默认typename为string
                    docsParameter.setTypeName(DocsContext.Parameter.TypeNameEnum.String.getName());
                    dataType.addDocsParameter(docsParameter);
                }
            }
            // 泛型的类不缓存
            if (actualCls.getGenericSuperclass() instanceof ParameterizedType || EmptyUtil.isEmpty(actualCls.getTypeParameters())) {
                docsDataTypes.put(dataType.getName(), dataType);
            }
            return dataType;
        }


        private Class<?> richParameterArray(@NonNull Class<?> cls, @NonNull Map<String, Type> genericTypeMap, @NonNull DocsContext.Parameter docsParameter) {
            int arrayCount = 0;
            Class actualCls = cls;
            if (actualCls == List.class) {
                // 泛型 T 的真实类型
                arrayCount++;
                Type type = genericTypeMap.get(actualCls.getTypeParameters()[0].getName());
                while (true) {
                    if (type instanceof ParameterizedType) {
                        if (((ParameterizedType) type).getRawType() == List.class) {
                            arrayCount++;
                        }
                        type = ((ParameterizedType) type).getActualTypeArguments()[0];
                    } else if (type instanceof TypeVariableImpl) {
                        type = genericTypeMap.get(type.getTypeName());
                    } else {
                        actualCls = (Class) type;
                        break;
                    }
                }
            }
            // [] 对象
            if (actualCls.isArray()) {
                while (true) {
                    Class<?> arrayCls = actualCls.getComponentType();
                    if (arrayCls == null) {
                        break;
                    }
                    arrayCount++;
                    actualCls = arrayCls;
                }
            }
            docsParameter.setArrayCount(arrayCount);
            docsParameter.setArray(arrayCount > 0);
            return actualCls;
        }

    }
}
