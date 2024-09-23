/*
 * Copyright (c) 2017～2099 Cowave All Rights Reserved.
 *
 * For licensing information, please contact: https://www.cowave.com.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 */
package com.cowave.commons.framework.access;

import com.cowave.commons.framework.helper.MessageHelper;
import com.cowave.commons.framework.helper.alarm.AccessAlarmFactory;
import com.cowave.commons.framework.helper.alarm.Alarm;
import com.cowave.commons.framework.helper.alarm.AlarmHandler;
import com.cowave.commons.tools.AssertsException;
import com.cowave.commons.tools.DateUtils;
import com.cowave.commons.tools.HttpException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.feign.codec.HttpResponse;
import org.springframework.feign.codec.Response;
import org.springframework.feign.codec.ResponseCode;
import org.springframework.feign.invoke.RemoteAssertsException;
import org.springframework.feign.invoke.RemoteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolationException;
import java.beans.PropertyEditorSupport;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static org.springframework.feign.codec.ResponseCode.*;

/**
 * @author shanhuiming
 */
@ConditionalOnMissingBean(HttpAccessAdvice.class)
@RequiredArgsConstructor
@RestControllerAdvice
public class AccessAdvice {

    // ErrorLog和Response.cause都不记内容
    private static final int ERR_LEVEL_0 = 0;

    // ErrorLog不记，Response.cause记录e.msg
    private static final int ERR_LEVEL_1 = 1;

    // ErrorLog和Response.cause都记e.msg
    private static final int ERR_LEVEL_2 = 2;

    // ErrorLog和Response.cause都记e.stack
    private static final int ERR_LEVEL_3 = 3;

    private final AccessLogger accessLogger;

    private final MessageHelper messageHelper;

    private final ThreadPoolExecutor applicationExecutor;

    @Nullable
    private final AlarmHandler alarmHandler;

    @Nullable
    private final AccessAlarmFactory<? extends Alarm> accessAlarmFactory;

    /**
     * 参数转换
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(DateUtils.parse(text));
            }
        });
    }

    @ExceptionHandler(HttpException.class)
    public HttpResponse<Map<String, String>> handleHttpException(HttpException e) {
        AccessLogger.error("", e);

        Map<String, String> body = Map.of("code", e.getCode(), "msg", e.getMessage());
        HttpResponse<Map<String, String>> httpResponse = new HttpResponse<>(e.getStatus(), null, body);

        httpResponse.setMessage(String.format("{code=%s, msg=%s}", e.getCode(), e.getMessage()));
        accessLogger.logResponse(httpResponse);

        processAccessAlarm(e.getStatus(), e.getCode(), e.getMessage(), httpResponse, e);
        return httpResponse;
    }

    @ExceptionHandler(AssertsException.class)
    public Response<Void> handleAssertsException(AssertsException e) {
        AccessLogger.error("", e);
        Response<Void> resp = Response.msg(SYS_ERROR, messageHelper.translateAssertsMessage(e));
        accessLogger.logResponse(resp);
        try {
            LinkedList<String> cause = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.toCollection(LinkedList::new));
            cause.addFirst(e.getMessage());
            resp.setCause(cause);
        } catch (Exception ex) {
            AccessLogger.error("", ex);
        }

        processAccessAlarm(HttpStatus.OK.value(), SYS_ERROR.getCode(), resp.getMsg(), resp, e);
        return resp;
    }

    @ExceptionHandler(RemoteAssertsException.class)
    public Response<Void> handleRemoteAssertsException(RemoteAssertsException e) {
        return error(e, SYS_ERROR, null, e.getMessage(), ERR_LEVEL_0);
    }

    @ExceptionHandler(RemoteException.class)
    public Response<Void> handleRemoteException(RemoteException e) {
        return error(e, SYS_ERROR, "frame.remote.failed", "远程调用失败", ERR_LEVEL_1);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Response<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return error(e, BAD_REQUEST, "frame.advice.httpRequestMethodNotSupportedException", "不支持的请求方法", ERR_LEVEL_2);
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    public Response<Void> handleHttpMessageConversionException(HttpMessageConversionException e) {
        return error(e, BAD_REQUEST, "frame.advice.httpMessageConversionException", "请求参数转换失败", ERR_LEVEL_2);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        return error(e, BAD_REQUEST, msg, msg, ERR_LEVEL_2);
    }

    @ExceptionHandler(BindException.class)
    public Response<Void> handleBindException(BindException e) {
        String msg = messageHelper.msg(e.getAllErrors().get(0).getDefaultMessage());
        return error(e, BAD_REQUEST, msg, msg, ERR_LEVEL_2);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Response<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getMessage().split(": ")[1];
        return error(e, BAD_REQUEST, msg, msg, ERR_LEVEL_2);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Response<Void> handleAccessDeniedException(AccessDeniedException e) {
        return error(e, FORBIDDEN, "frame.auth.denied", "没有访问权限", ERR_LEVEL_2);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Response<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return error(e, BAD_REQUEST, "frame.advice.illegalArgumentException", "非法参数", ERR_LEVEL_3);
    }

    @ExceptionHandler(SQLException.class)
    public Response<Void> handleSqlException(SQLException e) {
        return error(e, INTERNAL_SERVER_ERROR, "frame.advice.sqlException", "数据操作失败", ERR_LEVEL_3);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Response<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        return error(e, INTERNAL_SERVER_ERROR, "frame.advice.duplicateKeyException", "数据主键冲突", ERR_LEVEL_3);
    }

    @ExceptionHandler(DataAccessException.class)
    public Response<Void> handleDataAccessException(DataAccessException e) {
        return error(e, INTERNAL_SERVER_ERROR, "frame.advice.dataAccessException", "数据访问失败", ERR_LEVEL_3);
    }

    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(Exception e) {
        return error(e, INTERNAL_SERVER_ERROR, "frame.advice.exception", "系统错误", ERR_LEVEL_3);
    }

    private Response<Void> error(Exception e, ResponseCode code, String msgKey, String msg, int errLevel) {
        // 异常日志
        if(errLevel >= ERR_LEVEL_3){
            AccessLogger.error("", e);
        }else if(errLevel == ERR_LEVEL_2){
            AccessLogger.error(e.getMessage());
        }

        // 响应日志
        Response<Void> resp = Response.msg(code, messageHelper.translateErrorMessage(msgKey, msg));
        accessLogger.logResponse(resp);

        // 返回响应
        if(errLevel >= ERR_LEVEL_3){
            try {
                LinkedList<String> cause = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.toCollection(LinkedList::new));
                cause.addFirst(e.getMessage());
                resp.setCause(cause);
            } catch (Exception ex) {
                AccessLogger.error("", ex);
            }
        }else if(errLevel >= ERR_LEVEL_1){
            resp.setCause(List.of(e.getMessage()));
        }

        processAccessAlarm(HttpStatus.OK.value(), code.getCode(), resp.getMsg(), resp, e);
        return resp;
    }

    private void processAccessAlarm(int httpStatus, String code, String message, Object response, Exception e) {
        if (alarmHandler != null && accessAlarmFactory != null) {
            Alarm alarm = accessAlarmFactory.createAlarm(httpStatus, code, message, response, e);
            if(alarm.isAsync()){
                applicationExecutor.execute(() -> alarmHandler.handle(alarm));
            }else{
                alarmHandler.handle(alarm);
            }
        }
    }
}
