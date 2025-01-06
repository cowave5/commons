/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.configuration;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.HibernateValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.Validator;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 *
 * @author shanhuiming
 *
 */
@AutoConfigureBefore({MessageSourceAutoConfiguration.class})
public class I18nConfiguration {

    private static final String FRAMEWORK_MESSAGES = "META-INF/i18n/messages-frame";

    @Bean
    public MessageSource messageSource(@Value("${spring.messages.basename:}") String messages) {
        String[] array;
        if(StringUtils.isNotBlank(messages)){
            array = Stream.concat(
                    Arrays.stream(messages.split(",")), Stream.of(FRAMEWORK_MESSAGES)).toArray(String[]::new);
        }else{
            array = new String[]{FRAMEWORK_MESSAGES};
        }
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames(array);
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean localValidatorFactoryBean(MessageSource messageSource) {
        LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.setProviderClass(HibernateValidator.class);
        localValidatorFactoryBean.setValidationMessageSource(messageSource);
        return localValidatorFactoryBean;
    }

    @Bean
    public Validator validator(LocalValidatorFactoryBean localValidatorFactoryBean) {
        return localValidatorFactoryBean.getValidator();
    }
}
