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

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.*;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
@ConditionalOnClass(DiscoveryClient.class)
@ConditionalOnEnabledHealthIndicator("discovery")
@AutoConfiguration
public class DiscoveryHealthIndicator extends AbstractHealthIndicator {

    private final DiscoveryClient discoveryClient;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Map<String, List<String>> table = new TreeMap<>();
        List<String> services = discoveryClient.getServices();
        for(String name : services){
            List<String> list = table.computeIfAbsent(name, k -> new ArrayList<>());
            List<ServiceInstance> instances = discoveryClient.getInstances(name);
            for(ServiceInstance instance : instances){
                list.add(instance.getUri().toString());
            }
        }
        builder.up().withDetails(table);
    }
}
