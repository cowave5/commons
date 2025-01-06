/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.access.security;

import java.util.Collection;

import com.cowave.commons.framework.access.Access;

/**
 *
 * @author shanhuiming
 *
 */
public class AccessInfoParser {

    @SuppressWarnings("rawtypes")
    public void parse(Class<?> clazz, Object arg) {
        Access access = Access.get();
        if(access == null){
            return;
        }
        AccessUserDetails accessUserDetails = access.getUserDetails();
        if(accessUserDetails == null){
            return;
        }
        if(AccessInfo.class.isAssignableFrom(clazz)) {
            AccessInfo accessInfo = (AccessInfo)arg;
            accessInfo.setAccessUserId(accessUserDetails.getUserId());
            accessInfo.setAccessUserCode(accessUserDetails.getUserCode());
            accessInfo.setAccessUserAccount(accessUserDetails.getUsername());
            accessInfo.setAccessUserName(accessUserDetails.getUserNick());
            accessInfo.setAccessDeptId(accessUserDetails.getDeptId());
            accessInfo.setAccessDeptCode(accessUserDetails.getDeptCode());
            accessInfo.setAccessDeptName(accessUserDetails.getDeptName());
        }else if(Collection.class.isAssignableFrom(clazz)) {
            Collection col = (Collection)arg;
            for(Object o : col) {
                if(!AccessInfo.class.isAssignableFrom(o.getClass())) {
                    break;
                }
                AccessInfo accessInfo = (AccessInfo)arg;
                accessInfo.setAccessUserId(accessUserDetails.getUserId());
                accessInfo.setAccessUserCode(accessUserDetails.getUserCode());
                accessInfo.setAccessUserAccount(accessUserDetails.getUsername());
                accessInfo.setAccessUserName(accessUserDetails.getUserNick());
                accessInfo.setAccessDeptId(accessUserDetails.getDeptId());
                accessInfo.setAccessDeptCode(accessUserDetails.getDeptCode());
                accessInfo.setAccessDeptName(accessUserDetails.getDeptName());
            }
        }
    }
}
