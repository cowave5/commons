/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.operation;

import com.cowave.commons.framework.access.Access;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
@Component
public class OperationAspect {

    private final ExpressionParser exprParser = new SpelExpressionParser();

    private final ApplicationContext applicationContext;

    private final TaskExecutor taskExecutor;

    @Pointcut("@annotation(com.cowave.commons.framework.access.operation.Operation) " +
            "&& (@annotation(org.springframework.web.bind.annotation.RequestMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.GetMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.PostMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public void oplog() {

    }

    @AfterReturning(pointcut = "oplog() && @annotation(operation)", returning = "resp")
    public void doAfter(JoinPoint joinPoint, Operation operation, Object resp) {
        EvaluationContext context = new StandardEvaluationContext();
        // 参数信息
        Map<String, Object> argMap = new HashMap<>();
        // 操作信息
        OperationInfo operationInfo = new OperationInfo();
        boolean specifyHandle = prepareOperation(joinPoint, operation, argMap, operationInfo, context);
        operationInfo.setSuccess(true);
        operationInfo.setDesc(parseDesc(operation, context));
        context.setVariable("opInfo", operationInfo);
        context.setVariable("resp", resp);
        context.setVariable("exception", null);
        if (specifyHandle) {
            handleOperation(operation, context);
        } else {
            defaultHandle(operationInfo, argMap, resp, null);
        }
    }

    @AfterThrowing(pointcut = "oplog() && @annotation(operation)", throwing = "e")
    public void doThrow(JoinPoint joinPoint, Operation operation, Exception e) {
        EvaluationContext context = new StandardEvaluationContext();
        // 参数信息
        Map<String, Object> argMap = new HashMap<>();
        // 操作信息
        OperationInfo operationInfo = new OperationInfo();
        boolean specifyHandle = prepareOperation(joinPoint, operation, argMap, operationInfo, context);
        operationInfo.setSuccess(false);
        operationInfo.setDesc(parseDesc(operation, context));
        context.setVariable("opInfo", operationInfo);
        context.setVariable("resp", null);
        context.setVariable("exception", e);
        if (specifyHandle) {
            handleOperation(operation, context);
        } else {
            defaultHandle(operationInfo, argMap, null, e);
        }
    }

    public void defaultHandle(OperationInfo operationInfo, Map<String, Object> argMap, Object resp, Exception e){
        OperationHandler operationHandler;
        try{
            operationHandler = applicationContext.getBean(OperationHandler.class);
        }catch (Exception ex){
            throw new RuntimeException("Neither 'handleExpr' is specified nor 'OperationHandler' is implemented");
        }
        operationHandler.handle(operationInfo, argMap, resp, e);
    }

    private boolean prepareOperation(JoinPoint joinPoint, Operation operation,
                                     Map<String, Object> argMap, OperationInfo opInfo, EvaluationContext context){
        // 方法参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        // 方法参数名
        String[] paramNames = signature.getParameterNames();
        // 设置EvaluationContext
        if(paramNames != null) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
                if (args[i] != null) {
                    // 去掉一些不能序列化的参数，避免后面一些对argMap的操作失败
                    Class<?> clazz = args[i].getClass();
                    if (MultipartFile.class.isAssignableFrom(clazz)
                            || MultipartFile[].class.isAssignableFrom(clazz)
                            || HttpServletRequest.class.isAssignableFrom(clazz)
                            || HttpServletResponse.class.isAssignableFrom(clazz)
                            || BeanPropertyBindingResult.class.isAssignableFrom(clazz)
                            || ExtendedServletRequestDataBinder.class.isAssignableFrom(clazz)) {
                        continue;
                    }
                }
                argMap.put(paramNames[i], args[i]);
            }
        }

        // 设置OperationInfo
        opInfo.setAccessTime(Access.accessTime());
        opInfo.setAccessIp(Access.accessIp());
        opInfo.setAccessUrl(Access.accessUrl());
        opInfo.setUserId(Access.userId());
        opInfo.setUserCode(Access.userCode());
        opInfo.setDeptId(Access.deptId());
        opInfo.setDeptCode(Access.deptCode());
        opInfo.setOpType(operation.type());
        opInfo.setOpAction(operation.action());
        opInfo.setOpArgs(argMap);
        opInfo.setOpCost(System.currentTimeMillis() - Access.accessTime().getTime());

        // handleExpr
        String handleExpr = operation.handleExpr();
        if(StringUtils.isBlank(handleExpr)){
            return false;
        }

        int startIndex = handleExpr.indexOf("#") + 1;
        int endIndex = handleExpr.indexOf(".");
        if(startIndex == 0 || endIndex == -1){
            throw new RuntimeException("invalid handleExpr: " + handleExpr);
        }

        String handlerBean = handleExpr.substring(startIndex, endIndex);
        if(!applicationContext.containsBean(handlerBean)){
            throw new RuntimeException(" can't found bean of " + handlerBean + " in handleExpr: " + handleExpr);
        }

        // 处理方法
        context.setVariable(handlerBean, applicationContext.getBean(handlerBean));
        return true;
    }

    private String parseDesc(Operation operation, EvaluationContext context){
        String descSpel = operation.desc();
        if(StringUtils.isBlank(descSpel)){
            return "";
        }

        try{
            return exprParser.parseExpression(descSpel, new TemplateParserContext()).getValue(context, String.class);
        }catch(Exception e){
            log.error("", e);
            return "";
        }
    }

    private void handleOperation(Operation operation, EvaluationContext context){
        if(operation.isAsync()){
            taskExecutor.execute(() -> exprParser.parseExpression(operation.handleExpr()).getValue(context));
        }else{
            try{
                exprParser.parseExpression(operation.handleExpr()).getValue(context);
            }catch (Exception ex){
                log.error("", ex);
            }
        }
    }
}
