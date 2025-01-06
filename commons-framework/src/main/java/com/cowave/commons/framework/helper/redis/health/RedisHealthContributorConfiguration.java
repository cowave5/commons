/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.redis.health;

import java.util.Map;

import com.cowave.commons.framework.helper.redis.StringRedisHelper;
import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnEnabledHealthIndicator("redis")
@Configuration
public class RedisHealthContributorConfiguration
        extends CompositeHealthContributorConfiguration<RedisHealthIndicator, StringRedisHelper> {

    @Bean
    public HealthContributor redisHealthIndicator(Map<String, StringRedisHelper> redisMap) {
        return createContributor(redisMap);
    }
}
