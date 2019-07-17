package com.common.collect.debug.spring;

import com.alibaba.dubbo.remoting.TimeoutException;
import com.alibaba.dubbo.rpc.RpcException;
import com.common.collect.api.Response;
import com.common.collect.api.excps.UnifiedException;
import com.common.collect.container.aops.LogConstant;
import com.common.collect.container.trace.TraceIdUtil;
import com.common.collect.util.EmptyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

@ControllerAdvice
@ResponseBody
@Slf4j
public class ControllerErrorHandler {

    private static final String PARAM_ERROR_MESSAGE = "输入参数不合法";
    private static final String RPC_EXCEPTION = "rpc 服务调用失败";
    private static final String DB_EXCEPTION = "数据库异常";
    private static final String INTERNAL_EXCEPTION = "发生未知错误，请联系技术人员排查!";

    /**
     * Valid标签 校验失败
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response processControllerError(NativeWebRequest request, BindException ex) {
        printLogInfo(request, ex);
        List<FieldError> fieldErrors = ex.getFieldErrors();
        if (EmptyUtil.isEmpty(fieldErrors)) {
            return Response.fail(PARAM_ERROR_MESSAGE);
        }
        FieldError fieldError = fieldErrors.get(0);
        Response response = Response.fail(fieldError.getDefaultMessage());
        response.addContext("traceId", TraceIdUtil.traceId());
        return response;
    }

    /**
     * Validated标签 校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response processControllerError(NativeWebRequest request, ConstraintViolationException ex) {
        printLogInfo(request, ex);
        Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
        if (EmptyUtil.isEmpty(constraintViolations)) {
            return Response.fail(PARAM_ERROR_MESSAGE);
        }
        ConstraintViolation<?> next = constraintViolations.iterator().next();
        Response response = Response.fail(next.getMessage());
        response.addContext("traceId", TraceIdUtil.traceId());
        return response;
    }

    /**
     * required=true 校验失败
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response processControllerError(NativeWebRequest request, MissingServletRequestParameterException ex) {
        printLogInfo(request, ex);
        Response response = Response.fail(ex.getMessage());
        response.addContext("traceId", TraceIdUtil.traceId());
        return response;
    }

    /**
     * JSR303注解参数 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response processJsr303ValidatorError(NativeWebRequest request, MethodArgumentNotValidException ex) {
        printLogInfo(request, ex);
        BindingResult errors = ex.getBindingResult();
        if (EmptyUtil.isEmpty(errors.getFieldErrors())) {
            return Response.fail(PARAM_ERROR_MESSAGE);
        }
        FieldError fieldError = errors.getFieldErrors().get(0);
        Response response = Response.fail(fieldError.getDefaultMessage());
        response.addContext("traceId", TraceIdUtil.traceId());
        return response;
    }

    /**
     * 输入参数数据类型转换错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response processControllerError(NativeWebRequest request, HttpMessageNotReadableException ex) {
        printLogInfo(request, ex);
        Response response = Response.fail(PARAM_ERROR_MESSAGE);
        response.addContext("traceId", TraceIdUtil.traceId());
        return response;
    }

    /**
     * rpc 连接超时 rpc 异常
     */
    @ExceptionHandler({RpcException.class, TimeoutException.class})
    @ResponseStatus(HttpStatus.OK)
    public Response processControllerError(NativeWebRequest request, RpcException ex) {
        printLogInfo(request, ex);
        Response response = Response.fail(RPC_EXCEPTION);
        response.addContext("traceId", TraceIdUtil.traceId());
        return response;
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(UnifiedException.class)
    @ResponseStatus(HttpStatus.OK)
    public Response processControllerError(NativeWebRequest request, UnifiedException ex) {
        String message = ex.getErrorMessage();
        if (ex.getCause() != null) {
            printLogInfo(request, ex);
        }
        Response response = Response.fail(ex.getErrorCode(), message);
        response.addContext("traceId", TraceIdUtil.traceId());
        response.addContext(ex.getContext());
        return response;
    }

    /**
     * spring 封装的数据库异常 spring未捕获的数据库异常
     */
    @ExceptionHandler({SQLException.class, DataAccessException.class})
    @ResponseStatus(HttpStatus.OK)
    public Response processControllerError(NativeWebRequest request, SQLException ex) {
        printLogInfo(request, ex);
        Response response = Response.fail(DB_EXCEPTION);
        response.addContext("traceId", TraceIdUtil.traceId());
        return response;
    }

    /**
     * 兜底
     */
    @ExceptionHandler({RuntimeException.class, Exception.class, Error.class, Throwable.class})
    @ResponseStatus(HttpStatus.OK)
    public Response processControllerError(NativeWebRequest request, RuntimeException ex) {
        printLogInfo(request, ex);
        Response response = Response.fail(INTERNAL_EXCEPTION);
        response.addContext("traceId", TraceIdUtil.traceId());
        return response;
    }

    /**
     * 打印日志信息
     */
    private void printLogInfo(NativeWebRequest request, Throwable ex) {
        log.info("request parameters:{}", LogConstant.getObjString(request.getParameterMap()), ex);
    }
}