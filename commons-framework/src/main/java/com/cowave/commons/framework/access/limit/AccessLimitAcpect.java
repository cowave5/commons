/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.limit;

import com.cowave.commons.client.http.asserts.HttpHintException;
import com.cowave.commons.framework.access.Access;
import com.cowave.commons.framework.access.AccessProperties;
import com.cowave.commons.framework.configuration.ApplicationProperties;
import com.cowave.commons.tools.ServletUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

import static com.cowave.commons.client.http.constants.HttpCode.SUCCESS;
import static com.cowave.commons.client.http.constants.HttpCode.TOO_MANY_REQUESTS;

/**
 *
 * @author shanhuiming
 *
 */
@Aspect
@RequiredArgsConstructor
@Component
public class AccessLimitAcpect {

    private final ExpressionParser exprParser = new SpelExpressionParser();

    private final ApplicationProperties applicationProperties;

    private final AccessProperties accessProperties;

    private final AccessLimiter accessLimiter;

    @Pointcut("@annotation(com.cowave.commons.framework.access.limit.AccessLimit) " +
            "&& (@annotation(org.springframework.web.bind.annotation.RequestMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.GetMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.PostMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public void limitPoint() {

    }

    @Before("limitPoint()")
    public void limitRequest(JoinPoint point) {
        MethodSignature signature = (MethodSignature)point.getSignature();
        Method method = signature.getMethod();
        AccessLimit accessLimit = method.getAnnotation(AccessLimit.class);
        HttpServletRequest httpServletRequest = Access.httpRequest();
        // url
        assert httpServletRequest != null;
        String accessUrl = httpServletRequest.getRequestURI();
        String limitKey = applicationProperties.getLimitNamespace() + accessUrl;
        // ip
        if(accessLimit.limitWithIp()){
            String accessIp = ServletUtils.getRequestIp(httpServletRequest);
            limitKey = limitKey + ":" + accessIp;
        }
        // user
        if (accessLimit.limitWithUser()) {
            String accessUser = Access.userAccount();
            if (StringUtils.isNotBlank(accessUser)) {
                limitKey = limitKey + ":" + accessUser;
            }
        }
        // spel
        String keySpel = accessLimit.limitWithKey();
        if(StringUtils.isNotBlank(keySpel)){
            EvaluationContext context = new StandardEvaluationContext();
            Object[] args = point.getArgs();
            String[] paramNames = signature.getParameterNames();
            if(paramNames != null) {
                for (int i = 0; i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            String key = exprParser.parseExpression(keySpel, new TemplateParserContext()).getValue(context, String.class);
            limitKey = limitKey + ":" + key;
        }

        boolean throughLimit = accessLimiter.throughLimit(limitKey, accessLimit.period(), accessLimit.limits());
        if(throughLimit){
            return;
        }

        HttpServletResponse httpServletResponse = Access.httpResponse();
        assert httpServletResponse != null;
        long retryAfter = accessLimit.period() / 1000;
        httpServletResponse.setHeader("Retry-After", String.valueOf(retryAfter > 0 ? retryAfter : 1));
        if(accessProperties.isAlwaysSuccess()){
            throw new HttpHintException(SUCCESS.getStatus(), TOO_MANY_REQUESTS.getCode(), accessLimit.message());
        }else{
            throw new HttpHintException(TOO_MANY_REQUESTS, accessLimit.message());
        }
    }
}
