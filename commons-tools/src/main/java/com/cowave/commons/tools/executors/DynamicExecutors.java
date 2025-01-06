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

import org.dromara.dynamictp.common.queue.VariableLinkedBlockingQueue;
import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.dromara.dynamictp.core.executor.ScheduledDtpExecutor;
import org.dromara.dynamictp.core.support.ExecutorWrapper;

import java.util.concurrent.*;

/**
 *
 * @author shanhuiming
 *
 */
public class DynamicExecutors {

    public static ThreadPoolExecutor newThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        executor.setRejectHandlerType("CallerRunsPolicy"); // dtp代码实现这里不设置有问题
        registerDynamic(executor, executor.getThreadFactory());
        return executor;
    }

    public static ThreadPoolExecutor newThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        executor.setRejectHandlerType("CallerRunsPolicy");
        registerDynamic(executor, threadFactory);
        return executor;
    }

    public static ThreadPoolExecutor newThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        if(handler instanceof ThreadPoolExecutor.AbortPolicy){
            executor.setRejectHandlerType("AbortPolicy");
        }else if(handler instanceof ThreadPoolExecutor.DiscardPolicy ){
            executor.setRejectHandlerType("DiscardPolicy");
        }else if(handler instanceof ThreadPoolExecutor.DiscardOldestPolicy ){
            executor.setRejectHandlerType("DiscardOldestPolicy");
        }else{
            executor.setRejectHandlerType("CallerRunsPolicy");
        }
        registerDynamic(executor, executor.getThreadFactory());
        return executor;
    }

    public static ThreadPoolExecutor newThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        if(handler instanceof ThreadPoolExecutor.AbortPolicy){
            executor.setRejectHandlerType("AbortPolicy");
        }else if(handler instanceof ThreadPoolExecutor.DiscardPolicy ){
            executor.setRejectHandlerType("DiscardPolicy");
        }else if(handler instanceof ThreadPoolExecutor.DiscardOldestPolicy ){
            executor.setRejectHandlerType("DiscardOldestPolicy");
        }else{
            executor.setRejectHandlerType("CallerRunsPolicy");
        }
        registerDynamic(executor, threadFactory);
        return executor;
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        ScheduledDtpExecutor executor = new ScheduledDtpExecutor(corePoolSize, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new VariableLinkedBlockingQueue<>(1024), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        executor.setRejectHandlerType("AbortPolicy");
        registerDynamic(executor, executor.getThreadFactory());
        return executor;
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        ScheduledDtpExecutor executor = new ScheduledDtpExecutor(corePoolSize, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new VariableLinkedBlockingQueue<>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());
        executor.setRejectHandlerType("AbortPolicy");
        registerDynamic(executor, threadFactory);
        return executor;
    }

    public static ThreadPoolExecutor newSingleThreadPool() {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(1, 1,0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        executor.setRejectHandlerType("CallerRunsPolicy");
        registerDynamic(executor, executor.getThreadFactory());
        return executor;
    }

    public static ThreadPoolExecutor newSingleThreadPool(ThreadFactory threadFactory) {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(1, 1,0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
        executor.setRejectHandlerType("CallerRunsPolicy");
        registerDynamic(executor, threadFactory);
        return executor;
    }

    public static ThreadPoolExecutor newCachedThreadPool() {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
        executor.setRejectHandlerType("CallerRunsPolicy");
        registerDynamic(executor, executor.getThreadFactory());
        return executor;
    }

    public static ThreadPoolExecutor newCachedThreadPool(ThreadFactory threadFactory) {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
        executor.setRejectHandlerType("CallerRunsPolicy");
        registerDynamic(executor, threadFactory);
        return executor;
    }

    public static ThreadPoolExecutor newFixedThreadPool(int nThreads) {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        executor.setRejectHandlerType("CallerRunsPolicy");
        registerDynamic(executor, executor.getThreadFactory());
        return executor;
    }

    public static ThreadPoolExecutor newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        DynamicThreadPoolExecutor executor = new DynamicThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
        executor.setRejectHandlerType("CallerRunsPolicy");
        registerDynamic(executor, threadFactory);
        return executor;
    }

    private static void registerDynamic(DtpExecutor executor, ThreadFactory threadFactory){
        // 获取poolName
        String threadName = threadFactory.newThread(() -> {}).getName();
        if(threadName.contains("-thread-")){
            executor.setThreadPoolName(threadName.substring(0, threadName.lastIndexOf("-thread-")));
        }else if(threadName.contains("-")){
            executor.setThreadPoolName(threadName.substring(0, threadName.lastIndexOf('-')));
        }else{
            executor.setThreadPoolName(threadName);
        }
        // 注册到DtpRegistry
        ExecutorWrapper executorWrapper = new ExecutorWrapper(executor);
        DtpRegistry.registerExecutor(executorWrapper, executor.getThreadPoolName());
    }
}
