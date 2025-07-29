/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access;

import com.cowave.commons.client.http.asserts.*;
import com.cowave.commons.client.http.response.HttpResponse;
import com.cowave.commons.client.http.response.Response;
import com.cowave.commons.tools.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.beans.PropertyEditorSupport;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static com.cowave.commons.client.http.constants.HttpCode.*;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
@Conditional(MissingAdviceCondition.class)
@RestControllerAdvice
public class AccessAdvice {

    private static final int ERR_LEVEL_0 = 0;

    private static final int ERR_LEVEL_1 = 1;

    private static final int ERR_LEVEL_2 = 2;

    private static final int ERR_LEVEL_3 = 3;

    private final AccessLogger accessLogger;

    private final AccessProperties accessProperties;

    private final ObjectProvider<AccessExceptionHandler> exceptionHandlerProvider;

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

    /* ***************************************************************************
     * ERR_LEVEL_0 不打印异常日志，Response不记录cause
     * ***************************************************************************/

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public HttpResponse<Response<Void>> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return error(e, BAD_REQUEST.getStatus(), BAD_REQUEST.getCode(), ERR_LEVEL_0, I18Messages.msg("frame.advice.httpRequestMethodNotSupportedException"));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public HttpResponse<Response<Void>> handleUsernameNotFoundException(UsernameNotFoundException e) {
        return error(e, UNAUTHORIZED.getStatus(), UNAUTHORIZED.getCode(), ERR_LEVEL_0, I18Messages.msg("frame.auth.user.null"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public HttpResponse<Response<Void>> handleBadCredentialsException(BadCredentialsException e) {
        return error(e, UNAUTHORIZED.getStatus(), UNAUTHORIZED.getCode(), ERR_LEVEL_0, I18Messages.msg("frame.auth.pass.invalid"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public HttpResponse<Response<Void>> handleAccessDeniedException(AccessDeniedException e) {
        return error(e, FORBIDDEN.getStatus(), FORBIDDEN.getCode(), ERR_LEVEL_0, I18Messages.msg("frame.auth.permit.denied"));
    }

    @ExceptionHandler(HttpHintException.class)
    public HttpResponse<Response<Void>> handleHttpHintException(HttpHintException e) {
        return error(e, e.getStatus(), e.getCode(), ERR_LEVEL_0, e.getMessage());
    }

    /* ***************************************************************************
     * ERR_LEVEL_1 异常日志打印e.getMessage()，Response.cause记录e.getMessage()
     * ***************************************************************************/

    @ExceptionHandler(HttpWarnException.class)
    public HttpResponse<Response<Void>> handleHttpHttpWarnException(HttpWarnException e) {
        return error(e, e.getStatus(), e.getCode(), ERR_LEVEL_1, e.getMessage());
    }

    /* ***************************************************************************
     * ERR_LEVEL_2 异常日志打印堆栈，Response.cause记录e.getMessage()
     * ***************************************************************************/

    @ExceptionHandler(AuthenticationException.class)
    public HttpResponse<Response<Void>> handleAuthenticationException(AuthenticationException e) {
        return error(e, UNAUTHORIZED.getStatus(), UNAUTHORIZED.getCode(), ERR_LEVEL_2, I18Messages.msg("frame.auth.failed"));
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    public HttpResponse<Response<Void>> handleHttpMessageConversionException(HttpMessageConversionException e) {
        return error(e, BAD_REQUEST.getStatus(), BAD_REQUEST.getCode(), ERR_LEVEL_2, I18Messages.msg("frame.advice.httpMessageConversionException"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public HttpResponse<Response<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return error(e, BAD_REQUEST.getStatus(), BAD_REQUEST.getCode(), ERR_LEVEL_2, I18Messages.msg("frame.advice.methodArgumentTypeMismatchException"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public HttpResponse<Response<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        return error(e, BAD_REQUEST.getStatus(), BAD_REQUEST.getCode(), ERR_LEVEL_2, message);
    }

    @ExceptionHandler(BindException.class)
    public HttpResponse<Response<Void>> handleBindException(BindException e) {
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return error(e, BAD_REQUEST.getStatus(), BAD_REQUEST.getCode(), ERR_LEVEL_2, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public HttpResponse<Response<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getMessage().split(": ")[1];
        return error(e, BAD_REQUEST.getStatus(), BAD_REQUEST.getCode(), ERR_LEVEL_2, message);
    }

    /* ***************************************************************************
     * ERR_LEVEL_3 异常日志打印堆栈，Response.cause记录堆栈信息
     * ***************************************************************************/

    @ExceptionHandler(HttpException.class)
    public HttpResponse<Response<Void>> handleHttpException(HttpException e) {
        return error(e, e.getStatus(), e.getCode(), ERR_LEVEL_3, e.getMessage());
    }

    @ExceptionHandler(AssertsException.class)
    public HttpResponse<Response<Void>> handleAssertsException(AssertsException e) {
        return error(e, SERVICE_ERROR.getStatus(), SERVICE_ERROR.getCode(), ERR_LEVEL_3, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public HttpResponse<Response<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return error(e, INTERNAL_SERVER_ERROR.getStatus(), INTERNAL_SERVER_ERROR.getCode(), ERR_LEVEL_3, I18Messages.msg("frame.advice.illegalArgumentException"));
    }

    @ExceptionHandler(SQLException.class)
    public HttpResponse<Response<Void>> handleSqlException(SQLException e) {
        return error(e, INTERNAL_SERVER_ERROR.getStatus(), INTERNAL_SERVER_ERROR.getCode(), ERR_LEVEL_3, I18Messages.msg("frame.advice.sqlException"));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public HttpResponse<Response<Void>> handleDuplicateKeyException(DuplicateKeyException e) {
        return error(e, INTERNAL_SERVER_ERROR.getStatus(), INTERNAL_SERVER_ERROR.getCode(), ERR_LEVEL_3, I18Messages.msg("frame.advice.duplicateKeyException"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public HttpResponse<Response<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        return error(e, INTERNAL_SERVER_ERROR.getStatus(), INTERNAL_SERVER_ERROR.getCode(), ERR_LEVEL_3, I18Messages.msg("frame.advice.dataAccessInvalid"));
    }

    @ExceptionHandler(DataAccessException.class)
    public HttpResponse<Response<Void>> handleDataAccessException(DataAccessException e) {
        return error(e, INTERNAL_SERVER_ERROR.getStatus(), INTERNAL_SERVER_ERROR.getCode(), ERR_LEVEL_3, I18Messages.msg("frame.advice.dataAccessException"));
    }

    @ExceptionHandler(Exception.class)
    public HttpResponse<Response<Void>> handleException(Exception e) {
        return error(e, INTERNAL_SERVER_ERROR.getStatus(), INTERNAL_SERVER_ERROR.getCode(), ERR_LEVEL_3, I18Messages.msg("frame.advice.exception"));
    }

    private HttpResponse<Response<Void>> error(Exception e, int httpStatus, String code, int errLevel, String message) {
        // 异常日志
        if(errLevel >= ERR_LEVEL_2){
            AccessLogger.error("", e);
        }else if(errLevel == ERR_LEVEL_1 && e.getMessage() != null){
            AccessLogger.error(e.getMessage());
        }

        // Response
        Response<Void> response = new Response<>(code, message, null);
        if(errLevel == ERR_LEVEL_3){
            try {
                LinkedList<String> cause = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.toCollection(LinkedList::new));
                if(e.getMessage() != null){
                    cause.addFirst(e.getMessage());
                }
                response.setCause(cause);
            } catch (Exception ex) {
                AccessLogger.error("", ex);
            }
        }else if(errLevel >= ERR_LEVEL_1 && e.getMessage() != null){
            response.setCause(List.of(e.getMessage()));
        }

        HttpServletResponse servletResponse = Access.httpResponse();
        if(servletResponse.isCommitted()) {
            return null;
        }

        // HttpResponse
        if(accessProperties.isAlwaysSuccess()){
            httpStatus = SUCCESS.getStatus();
        }
        HttpResponse<Response<Void>> httpResponse = new HttpResponse<>(httpStatus, null, response);
        httpResponse.setMessage(String.format("{code=%s, msg=%s}", code, message));

        try {
            // 响应日志
            accessLogger.logResponse(httpResponse);
            // 自定义处理，比如告警
            AccessExceptionHandler exceptionHandler = exceptionHandlerProvider.getIfAvailable();
            if (exceptionHandler != null) {
                exceptionHandler.handler(e, httpStatus, response);
            }
        } catch (Exception ex) {
            AccessLogger.error("", ex);
        }
        return httpResponse;
    }
}
