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
import org.bson.Document;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnEnabledHealthIndicator("mongo")
@AutoConfiguration(after = { MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class MongoHealthIndicator extends AbstractHealthIndicator {

    private final MongoTemplate mongoTemplate;

    private final MongoProperties mongoProperties;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Document hostInfo = mongoTemplate.executeCommand("{ hostInfo: 1 }");
        Document system = hostInfo.get("system", Document.class);
        String host = system.getString("hostname");

        Document os = hostInfo.get("os", Document.class);
        String name = os.getString("type") + "_" + os.get("name") + "_" + os.get("version");

        Map<String, String> detail = new HashMap<>();
        detail.put("os", name);
        detail.put("host", host);
        detail.put("uri", mongoProperties.getUri());
        detail.put("database", mongoProperties.getDatabase());
        builder.up().withDetails(detail);
    }
}
