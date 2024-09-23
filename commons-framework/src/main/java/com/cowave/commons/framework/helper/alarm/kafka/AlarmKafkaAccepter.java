/*
 * Copyright (c) 2017～2099 Cowave All Rights Reserved.
 *
 * For licensing information, please contact: https://www.cowave.com.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 */
package com.cowave.commons.framework.helper.alarm.kafka;

import com.alibaba.fastjson.JSON;

import com.cowave.commons.framework.helper.alarm.Alarm;
import com.cowave.commons.framework.helper.alarm.AlarmAccepter;
import com.cowave.commons.framework.helper.alarm.AlarmAccepterConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
public class AlarmKafkaAccepter<T extends Alarm> implements AlarmAccepter<T> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final AlarmAccepterConfiguration accepterConfiguration;

    @Override
    public void accept(T alarm) {
        kafkaTemplate.send(accepterConfiguration.getKafkaTopic(), JSON.toJSONString(alarm));
    }
}
