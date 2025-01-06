/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.ids;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花算法
 * 由于js long最大53位，而雪花算法使用64位，差11位，导致前端long溢出
 * 这里使用改进算法
 * 雪花算法中，workId 10位，sequence 12位
 * 这里将workId改成5位，sequence改成6位，空出long的前11位，避免溢出
 *
 * @author jiangbo
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SnowFlakeId {

    private static final long SEED_TIME_STAMP = 1712113321438L;

    private static final int WORKER_ID_BITS = 5;

    private static final int SEQUENCE_BITS = 6;

    private static final int WORKER_ID_SHIFT = SEQUENCE_BITS;

    private static final int TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private static final int SEQUENCE_MASK = ~(-1 << SEQUENCE_BITS);

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private static final AtomicLong LAST_TIMESTAMP = new AtomicLong(-1);

    private long workerId = 1;

    private static final SnowFlakeId INSTANCE = new SnowFlakeId();

    public void setWorkerId(int workerId) {
        this.workerId = workerId;
    }

    public static synchronized String generatorStr() {
        return generator().toString();
    }

    /**
     * 生成带时间序列的ID
     */
    public static synchronized Long generator() {
        long timestamp = timeGen();

        long savedTimestamp = LAST_TIMESTAMP.get();
        if (timestamp < savedTimestamp) {
            // 时钟回退，加1毫秒
            timestamp = savedTimestamp + 1;
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        int seq = 0;
        if (savedTimestamp == timestamp) {
            seq = SEQUENCE.incrementAndGet() & SEQUENCE_MASK;

            // 毫秒内序列溢出
            if (seq == 0) {
                timestamp = tilNextMillis(savedTimestamp);
            }
        } else {
            seq = 0;
        }

        SEQUENCE.set(seq);
        LAST_TIMESTAMP.set(timestamp);

        // 用时间戳在前能得到更大的区间
        return ((timestamp - SEED_TIME_STAMP) << TIMESTAMP_LEFT_SHIFT)
                | (INSTANCE.workerId << WORKER_ID_SHIFT)
                | seq;
    }

    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private static long timeGen() {
        return System.currentTimeMillis();
    }
}

