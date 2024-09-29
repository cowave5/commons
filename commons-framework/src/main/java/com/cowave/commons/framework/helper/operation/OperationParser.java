/*
 * Copyright (c) 2017～2099 Cowave All Rights Reserved.
 *
 * For licensing information, please contact: https://www.cowave.com.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 */
package com.cowave.commons.framework.helper.operation;

import java.lang.reflect.Method;
import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
public interface OperationParser {

	void parseRequestContent(Method method, Map<String, Object> args, OperationLog log);

	void parseResponseContent(Method method, Object resp, OperationLog log);

	void parseExceptionContent(Method method, Exception e, OperationLog log);
}