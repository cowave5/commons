/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.actuator.health;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisAutoConfiguration;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
@ConditionalOnClass(ConnectionFactory.class)
@ConditionalOnBean(ConnectionFactory.class)
@ConditionalOnEnabledHealthIndicator("jms")
@AutoConfiguration(after = { ActiveMQAutoConfiguration.class, ArtemisAutoConfiguration.class })
public class JmsHealthIndicator extends AbstractHealthIndicator {

    private final ConnectionFactory connectionFactory;

    private final ActiveMQProperties activeMQProperties;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try (Connection connection = this.connectionFactory.createConnection()) {
            try{
                connection.start();
                builder.up();
            }catch (Exception e){
                builder.down();
            }
            Map<String, String> detail = new HashMap<>();
            detail.put("provider", connection.getMetaData().getJMSProviderName());
            detail.put("url", activeMQProperties.getBrokerUrl());
            builder.withDetails(detail);
        }
    }
}
