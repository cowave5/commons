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

import lombok.Data;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * 操作属性
 *
 * @author shanhuiming
 */
@Data
public class OperationInfo {

    /**
     * accessTime
     */
    private Date accessTime;

    /**
     * accessIp
     */
    private String accessIp;

    /**
     * accessUrl
     */
    private String accessUrl;

    /**
     * accessMethod
     */
    private String accessMethod;

    /**
     * userId
     */
    private Object userId;

    /**
     * userCode
     */
    private Object userCode;

    /**
     * userName
     */
    private String userName;

    /**
     * userAccount
     */
    private String userAccount;

    /**
     * deptId
     */
    private Object deptId;

    /**
     * deptCode
     */
    private Object deptCode;

    /**
     * deptName
     */
    private String deptName;

    /**
     * 请求参数
     */
    private Map<String, Object> opArgs;

    /**
     * 操作模块
     */
    private String opModule;

    /**
     * 操作类型
     */
    private String opType;

    /**
     * 操作动作
     */
    private String opAction;

    /**
     * 操作处理标识
     */
    private String opFlag;

    /**
     * 操作耗时
     */
    private long opCost;

    /**
     * 操作描述
     */
    private String desc;

    /**
     * 操作是否成功
     */
    private boolean success;

    public <T> T getUserId(){
        return (T)userId;
    }

    public <T> T getUserId(Function<Object, T> converter) {
        return converter.apply(userId);
    }

    public <T> T getUserCode(){
        return (T)userCode;
    }

    public <T> T getUserCode(Function<Object, T> converter) {
        return converter.apply(userCode);
    }

    public <T> T getDeptId(){
        return (T)deptId;
    }

    public <T> T getDeptId(Function<Object, T> converter) {
        return converter.apply(deptId);
    }

    public <T> T getDeptCode(){
        return (T)deptCode;
    }

    public <T> T getDeptCode(Function<Object, T> converter) {
        return converter.apply(deptCode);
    }
}
