/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.logging;

import ch.qos.logback.core.rolling.TriggeringPolicy;
import ch.qos.logback.core.rolling.helper.ArchiveRemover;
import ch.qos.logback.core.spi.ContextAware;

/**
 * @author shanhuiming
 * @see ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy
 */
public interface TimeBasedFileNamingAndTriggeringPolicy<E> extends TriggeringPolicy<E>, ContextAware {
    void setTimeBasedRollingPolicy(TimeBasedRollingPolicy<E> var1);

    String getElapsedPeriodsFileName();

    String getCurrentPeriodsFileNameWithoutCompressionSuffix();

    ArchiveRemover getArchiveRemover();

    long getCurrentTime();

    void setCurrentTime(long var1);
}
