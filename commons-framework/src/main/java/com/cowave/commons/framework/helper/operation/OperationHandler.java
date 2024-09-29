/*
 * Copyright (c) 2017～2099 Cowave All Rights Reserved.
 *
 * For licensing information, please contact: https://www.cowave.com.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 */
package com.cowave.commons.framework.helper.operation;

/**
 *
 * @author shanhuiming
 *
 */
public interface OperationHandler<T extends OperationLog> {

	void handle(T log);
}
