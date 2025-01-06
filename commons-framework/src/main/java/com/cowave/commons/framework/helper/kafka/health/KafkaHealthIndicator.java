/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.kafka.health;

import lombok.Data;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author shanhuiming
 *
 */
public class KafkaHealthIndicator extends AbstractHealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        List<String> nodes = new ArrayList<>();
        try(AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())){
            Collection<Node> nodeCollection =  adminClient.describeCluster().nodes().get(5, TimeUnit.SECONDS);
            nodeCollection.forEach(node -> nodes.add(node.host() + ":" + node.port()));

            List<String> groups = adminClient.listConsumerGroups().valid()
                    .get(5, TimeUnit.SECONDS).stream().map(ConsumerGroupListing::groupId).toList();
            Map<String, ConsumerGroupDescription> groupDetails =
                    adminClient.describeConsumerGroups(groups).all().get(5, TimeUnit.SECONDS);

            Map<String, ConsumerInfo> consumerMap = new HashMap<>();
            for(Map.Entry<String, ConsumerGroupDescription> entry: groupDetails.entrySet()){
                ConsumerGroupDescription group = entry.getValue();
                Collection<MemberDescription> members = group.members();
                for(MemberDescription member : members){
                    MemberAssignment assign = member.assignment();
                    String host = member.host().replace("/", "");
                    ConsumerInfo consumerInfo = consumerMap.computeIfAbsent(host, key -> new ConsumerInfo());
                    consumerInfo.getGroups().add(group.groupId());
                    Set<TopicPartition> partitions = assign.topicPartitions();
                    for(TopicPartition partition : partitions){
                        consumerInfo.getTopics().add(partition.topic());
                    }
                }
            }

            Map<String, Object> info = new HashMap<>();
            info.put("nodes", nodes);
            info.put("consumers", consumerMap);
            builder.up().withDetails(info);
        }catch(Exception e){
            builder.down();
        }
    }

    @Data
    public static class ConsumerInfo {

        private Set<String> groups = new HashSet<>();

        private Set<String> topics = new HashSet<>();
    }
}
