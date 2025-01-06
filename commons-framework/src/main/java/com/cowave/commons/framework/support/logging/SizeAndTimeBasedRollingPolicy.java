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

import ch.qos.logback.core.util.FileSize;

/**
 * @author shanhuiming
 * @see ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
 */
public class SizeAndTimeBasedRollingPolicy<E> extends TimeBasedRollingPolicy<E> {
    FileSize maxFileSize;

    public SizeAndTimeBasedRollingPolicy() {
    }

    public void start() {
        SizeAndTimeBasedFNATP<E> sizeAndTimeBasedFNATP = new SizeAndTimeBasedFNATP(SizeAndTimeBasedFNATP.Usage.EMBEDDED);
        if (this.maxFileSize == null) {
            this.addError("maxFileSize property is mandatory.");
        } else {
            this.addInfo("Archive files will be limited to [" + this.maxFileSize + "] each.");
            sizeAndTimeBasedFNATP.setMaxFileSize(this.maxFileSize);
            this.timeBasedFileNamingAndTriggeringPolicy = sizeAndTimeBasedFNATP;
            if (!this.isUnboundedTotalSizeCap() && this.totalSizeCap.getSize() < this.maxFileSize.getSize()) {
                this.addError("totalSizeCap of [" + this.totalSizeCap + "] is smaller than maxFileSize [" + this.maxFileSize + "] which is non-sensical");
            } else {
                super.start();
            }
        }
    }

    public void setMaxFileSize(FileSize aMaxFileSize) {
        this.maxFileSize = aMaxFileSize;
    }

    public String toString() {
        return "com.cowave.rolling.SizeAndTimeBasedRollingPolicy@" + this.hashCode();
    }
}
