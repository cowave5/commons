/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author shanhuiming
 *
 */
@Data
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties("spring.access")
public class AccessConfiguration {

    /**
     * AccessResponse是否永远200
     */
    private boolean alwaysSuccess;

    /**
     * AccessFilter拦截的url
     */
    private String[] patterns = {"/*"};

    /**
     * Access鉴权配置
     */
    private TokenConfig token;

    public String tokenHeader(){
        if(token != null){
            return token.header;
        }
        return "Authorization";
    }

    public String tokenSalt(){
        if(token != null){
            return token.salt;
        }
        return "admin@cowave.com";
    }
    public boolean tokenConflict(){
        if(token != null){
            return token.conflict;
        }
        return false;
    }

    public int tokenAccessExpire(){
        if(token != null){
            return token.accessExpire;
        }
        return 3600;
    }

    public int tokenRefreshExpire(){
        if(token != null){
            return token.refreshExpire;
        }
        return 3600 * 24 * 7;
    }

    public List<String> tokenIgnoreUrls(){
        if(token != null){
            return token.ignoreUrls;
        }
        return new ArrayList<>();
    }

    @Data
    public static class TokenConfig {

        /**
         * header名称
         */
        private String header = "Authorization";

        /**
         * 秘钥
         */
        private String salt = "admin@cowave.com";

        /**
         * 是否检查冲突
         */
        private boolean conflict = false;

        /**
         * accessToken超时
         */
        private int accessExpire = 3600;

        /**
         * refreshToken超时
         */
        private int refreshExpire = 3600 * 24 * 7;

        /**
         * 忽略鉴权的url
         */
        private List<String> ignoreUrls = new ArrayList<>();
    }
}
