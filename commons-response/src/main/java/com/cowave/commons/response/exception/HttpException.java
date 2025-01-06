/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.response.exception;

import com.cowave.commons.response.ResponseCode;
import lombok.Getter;

import static com.cowave.commons.response.HttpResponseCode.SERVICE_ERROR;

/**
 *
 * @author shanhuiming
 *
 */
@Getter
public class HttpException extends RuntimeException {

    private final int status;

    private final String code;

    public HttpException(String message, Object... args) {
        super(Messages.translateIfNeed(message, args));
        this.code = SERVICE_ERROR.getCode();
        this.status = SERVICE_ERROR.getStatus();
    }

    public HttpException(Throwable cause, String message, Object... args) {
        super(Messages.translateIfNeed(message, args), cause);
        this.code = SERVICE_ERROR.getCode();
        this.status = SERVICE_ERROR.getStatus();
    }

    public HttpException(ResponseCode responseCode) {
        super(Messages.translateIfNeed(responseCode.getMsg()));
        this.code = responseCode.getCode();
        this.status = responseCode.getStatus();
    }

    public HttpException(ResponseCode responseCode, String message, Object... args) {
        super(Messages.translateIfNeed(message, args));
        this.code = responseCode.getCode();
        this.status = responseCode.getStatus();
    }

    public HttpException(ResponseCode responseCode, Throwable cause, String message, Object... args) {
        super(Messages.translateIfNeed(message, args), cause);
        this.code = responseCode.getCode();
        this.status = responseCode.getStatus();
    }

    public HttpException(int status, String code) {
        this.code = code;
        this.status = status;
    }

    public HttpException(int status, String code, String message, Object... args) {
        super(Messages.translateIfNeed(message, args));
        this.code = code;
        this.status = status;
    }

    public HttpException(int status, String code, Throwable cause, String message, Object... args) {
        super(Messages.translateIfNeed(message, args), cause);
        this.code = code;
        this.status = status;
    }
}
