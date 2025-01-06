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

import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.rolling.helper.TimeBasedArchiveRemover;

import java.io.File;
import java.util.Date;

/**
 * @author shanhuiming
 * @see ch.qos.logback.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy
 */
@NoAutoStart
public class DefaultTimeBasedFileNamingAndTriggeringPolicy<E> extends TimeBasedFileNamingAndTriggeringPolicyBase<E> {
    public DefaultTimeBasedFileNamingAndTriggeringPolicy() {
    }

    public void start() {
        super.start();
        if (super.isErrorFree()) {
            if (this.tbrp.fileNamePattern.hasIntegerTokenCOnverter()) {
                this.addError("Filename pattern [" + this.tbrp.fileNamePattern + "] contains an integer token converter, i.e. %i, INCOMPATIBLE with this configuration. Remove it.");
            } else {
                this.archiveRemover = new TimeBasedArchiveRemover(this.tbrp.fileNamePattern, this.rc);
                this.archiveRemover.setContext(this.context);
                this.started = true;
            }
        }
    }

    public boolean isTriggeringEvent(File activeFile, E event) {
        long time = this.getCurrentTime();
        if (time >= this.nextCheck) {
            Date dateOfElapsedPeriod = this.dateInCurrentPeriod;
            this.addInfo("Elapsed period: " + dateOfElapsedPeriod);
            this.elapsedPeriodsFileName = this.tbrp.fileNamePatternWithoutCompSuffix.convert(dateOfElapsedPeriod);
            this.setDateInCurrentPeriod(time);
            this.computeNextCheck();
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "com.cowave.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy";
    }
}
