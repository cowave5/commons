/*
 * Copyright (c) 2017～2099 Cowave All Rights Reserved.
 *
 * For licensing information, please contact: https://www.cowave.com.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 */
package com.cowave.commons.tools;

import lombok.Getter;

/**
 *
 * @author shanhuiming
 *
 */
@Getter
public class HttpException extends RuntimeException {

    private final int status;

    private final String code;

    public HttpException(int status, String code, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public HttpException(int status, String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }
}