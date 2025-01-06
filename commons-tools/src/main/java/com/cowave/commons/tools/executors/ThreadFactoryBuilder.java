/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.executors;

import lombok.extern.slf4j.Slf4j;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author jiangbo
 */
@Slf4j
public class ThreadFactoryBuilder {

    private String nameFormat;

    private boolean daemon = true;

    private Integer priority;

    private ThreadFactory backingThreadFactory;

    private UncaughtExceptionHandler uncaughtExceptionHandler;

    private ThreadFactoryBuilder() {
    }

    public static ThreadFactoryBuilder apply() {
        return new ThreadFactoryBuilder();
    }

    public ThreadFactoryBuilder nameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
        return this;
    }

    public ThreadFactoryBuilder daemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    public ThreadFactoryBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    public ThreadFactoryBuilder uncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    public ThreadFactoryBuilder threadFactory(ThreadFactory backingThreadFactory) {
        this.backingThreadFactory = backingThreadFactory;
        return this;
    }

    public ThreadFactory build() {
        return build(this);
    }

    private static ThreadFactory build(ThreadFactoryBuilder builder) {
        String nameFormat = builder.nameFormat;
        boolean daemon = builder.daemon;
        Integer priority = builder.priority;
        UncaughtExceptionHandler uncaughtExceptionHandler = builder.uncaughtExceptionHandler;

        ThreadFactory backingThreadFactory = (builder.backingThreadFactory != null) ? builder.backingThreadFactory : Executors.defaultThreadFactory();
        AtomicLong count = (nameFormat != null) ? new AtomicLong(0) : null;
        return ((Runnable runnable) -> {
            Thread thread = backingThreadFactory.newThread(runnable);
            thread.setDaemon(daemon);
            if (nameFormat != null) {
                thread.setName(String.format(nameFormat, count.getAndIncrement()));
            }
            if (priority != null) {
                thread.setPriority(priority);
            }
            if (uncaughtExceptionHandler != null) {
                thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            } else {
                thread.setUncaughtExceptionHandler((Thread t, Throwable e) -> log.error("Thread exited anomaly", e));
            }
            return thread;
        });
    }
}
