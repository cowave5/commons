/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.kafka;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.converter.RecordMessageConverter;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnClass(KafkaTemplate.class)
@AutoConfigureBefore(org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class)
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaAutoConfiguration {

    @ConditionalOnMissingBean
    @Primary
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> kafkaListenerContainerFactory(KafkaProperties kafkaProperties) {
        checkAuthProperties(kafkaProperties);
        ConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @ConditionalOnMissingBean
    @Primary
    @Bean
    public ProducerFactory<?, ?> producerFactory(KafkaProperties kafkaProperties, ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers) {
        checkAuthProperties(kafkaProperties);
        DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
        String transactionIdPrefix = kafkaProperties.getProducer().getTransactionIdPrefix();
        if (transactionIdPrefix != null) {
            factory.setTransactionIdPrefix(transactionIdPrefix);
        }
        customizers.orderedStream().forEach(customizer -> customizer.customize(factory));
        return factory;
    }

    @ConditionalOnMissingBean
    @Primary
    @Bean
    public KafkaTemplate<?, ?> kafkaTemplate(KafkaProperties kafkaProperties, ProducerFactory<Object, Object> kafkaProducerFactory,
                                             ProducerListener<Object, Object> kafkaProducerListener, ObjectProvider<RecordMessageConverter> messageConverter) {
        KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory);
        messageConverter.ifUnique(kafkaTemplate::setMessageConverter);
        PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
        propertyMapper.from(kafkaProducerListener).to(kafkaTemplate::setProducerListener);
        propertyMapper.from(kafkaProperties.getTemplate().getDefaultTopic()).to(kafkaTemplate::setDefaultTopic);
        propertyMapper.from(kafkaProperties.getTemplate().getTransactionIdPrefix()).to(kafkaTemplate::setTransactionIdPrefix);
        return kafkaTemplate;
    }

    @ConditionalOnMissingBean
    @Primary
    @Bean
    public KafkaAdmin kafkaAdmin(KafkaProperties kafkaProperties) {
        checkAuthProperties(kafkaProperties);
        KafkaAdmin kafkaAdmin = new KafkaAdmin(kafkaProperties.buildAdminProperties());
        kafkaAdmin.setFatalIfBrokerNotAvailable(kafkaProperties.getAdmin().isFailFast());
        return kafkaAdmin;
    }

    @Conditional(CommonKafkaCondition.class)
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> commonKafkaListenerContainerFactory(Environment environment) {
        KafkaProperties kafkaProperties = Binder.get(environment).bind("common.kafka", KafkaProperties.class).get();
        checkAuthProperties(kafkaProperties);
        ConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Conditional(CommonKafkaCondition.class)
    @Bean
    public ProducerFactory<?, ?> commonProducerFactory(Environment environment, ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers) {
        KafkaProperties kafkaProperties = Binder.get(environment).bind("common.kafka", KafkaProperties.class).get();
        checkAuthProperties(kafkaProperties);
        DefaultKafkaProducerFactory<?, ?> factory = new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
        String transactionIdPrefix = kafkaProperties.getProducer().getTransactionIdPrefix();
        if (transactionIdPrefix != null) {
            factory.setTransactionIdPrefix(transactionIdPrefix);
        }
        customizers.orderedStream().forEach(customizer -> customizer.customize(factory));
        return factory;
    }

    @Conditional(CommonKafkaCondition.class)
    @Bean
    public KafkaTemplate<?, ?> commonKafkaTemplate(Environment environment, @Qualifier("commonProducerFactory") ProducerFactory<Object, Object> producerFactory,
                                                   ProducerListener<Object, Object> kafkaProducerListener, ObjectProvider<RecordMessageConverter> messageConverter) {
        KafkaProperties kafkaProperties = Binder.get(environment).bind("common.kafka", KafkaProperties.class).get();
        KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        messageConverter.ifUnique(kafkaTemplate::setMessageConverter);
        PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
        propertyMapper.from(kafkaProducerListener).to(kafkaTemplate::setProducerListener);
        propertyMapper.from(kafkaProperties.getTemplate().getDefaultTopic()).to(kafkaTemplate::setDefaultTopic);
        propertyMapper.from(kafkaProperties.getTemplate().getTransactionIdPrefix()).to(kafkaTemplate::setTransactionIdPrefix);
        return kafkaTemplate;
    }

    @Conditional(CommonKafkaCondition.class)
    @Bean
    public KafkaAdmin commonKafkaAdmin(Environment environment) {
        KafkaProperties kafkaProperties = Binder.get(environment).bind("common.kafka", KafkaProperties.class).get();
        checkAuthProperties(kafkaProperties);
        KafkaAdmin kafkaAdmin = new KafkaAdmin(kafkaProperties.buildAdminProperties());
        kafkaAdmin.setFatalIfBrokerNotAvailable(kafkaProperties.getAdmin().isFailFast());
        return kafkaAdmin;
    }

    private void checkAuthProperties(KafkaProperties properties) {
        String userName = properties.getProperties().remove("username");
        if (StringUtils.isBlank(userName)) {
            properties.getProperties().clear();
        }
    }
}
