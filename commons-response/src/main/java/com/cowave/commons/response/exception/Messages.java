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

import com.alibaba.ttl.TransmittableThreadLocal;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
public class Messages {

    private static final ThreadLocal<Locale> LANGUAGE = new TransmittableThreadLocal<>();

    private static MessageSource messageSource;

    public static void setMessageSource(MessageSource messageSource) {
        Messages.messageSource = messageSource;
    }

    public static void setLanguage(String language){
        if(StringUtils.isNotBlank(language)){
            if(language.toLowerCase().contains("en")) {
                LANGUAGE.set(new Locale("en", "US"));
            }else if(language.toLowerCase().contains("zh")) {
                LANGUAGE.set(new Locale("zh", "CN"));
            }
        }
    }

    public static Locale getLanguage() {
        Locale local = LANGUAGE.get();
        if(local != null){
            return local;
        }
        return Locale.CHINA;
    }

    public static void clearLanguage() {
        LANGUAGE.remove();
    }

    public static String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, key, getLanguage());
    }

    public static String msgWithDefault(String key, String defaultMessage, Object... args) {
        return messageSource.getMessage(key, args, defaultMessage, getLanguage());
    }

    public static String translateIfNeed(String message, Object... args){
        if(StringUtils.isBlank(message)){
            return "";
        }
        if (message.startsWith("{") && message.endsWith("}")) {
            message = message.substring(1, message.length() - 1);
            return messageSource.getMessage(message, args, message, getLanguage());
        }
        return message;
    }

    public static String translateAndReplace(String key, Map<String, Object> args, String prefix, String suffix) {
        if(StringUtils.isBlank(key)){
            return "";
        }

        String msg = messageSource.getMessage(key, null, key, getLanguage());
        if(MapUtils.isNotEmpty(args)){
            StringSubstitutor substitutor = new StringSubstitutor(args, prefix, suffix);
            return substitutor.replace(msg);
        }
        return msg;
    }
}
