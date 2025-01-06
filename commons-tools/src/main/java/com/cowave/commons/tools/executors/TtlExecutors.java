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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @author jiangbo
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TtlExecutors {

    public static void exec(Runnable runnable) {
        ThreadPoolExecutor executor = new TtlThreadPoolExecutor(0, 1,0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        executor.execute(runnable);
    }

    public static ThreadPoolExecutor newSingleThreadPool(ThreadFactory threadFactory) {
        ThreadPoolExecutor executor = new TtlThreadPoolExecutor(1, 1,0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));
        return executor;
    }

    public static ThreadPoolExecutor newCachedThreadPool(ThreadFactory threadFactory) {
        ThreadPoolExecutor executor = new TtlThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));
        return executor;
    }

    public static ThreadPoolExecutor newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        ThreadPoolExecutor executor = new TtlThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));
        return executor;
    }

    public static ScheduledThreadPoolExecutor newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactory);
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));
        return executor;
    }

    public static ScheduledThreadPoolExecutor newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));
        return executor;
    }
}
