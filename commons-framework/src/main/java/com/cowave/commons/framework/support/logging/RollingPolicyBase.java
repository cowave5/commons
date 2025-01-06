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

import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.FileNamePattern;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 *
 * @author shanhuiming
 * @see ch.qos.logback.core.rolling.RollingPolicyBase
 *
 */
public abstract class RollingPolicyBase extends ContextAwareBase implements RollingPolicy {
    protected CompressionMode compressionMode;
    FileNamePattern fileNamePattern;
    protected String fileNamePatternStr;
    private FileAppender<?> parent;
    FileNamePattern zipEntryFileNamePattern;
    private boolean started;

    public RollingPolicyBase() {
        this.compressionMode = CompressionMode.NONE;
    }

    protected void determineCompressionMode() {
        if (this.fileNamePatternStr.endsWith(".gz")) {
            this.addInfo("Will use gz compression");
            this.compressionMode = CompressionMode.GZ;
        } else if (this.fileNamePatternStr.endsWith(".zip")) {
            this.addInfo("Will use zip compression");
            this.compressionMode = CompressionMode.ZIP;
        } else {
            this.addInfo("No compression will be used");
            this.compressionMode = CompressionMode.NONE;
        }

    }

    public void setFileNamePattern(String fnp) {
        this.fileNamePatternStr = fnp;
    }

    public String getFileNamePattern() {
        return this.fileNamePatternStr;
    }

    public CompressionMode getCompressionMode() {
        return this.compressionMode;
    }

    public boolean isStarted() {
        return this.started;
    }

    public void start() {
        this.started = true;
    }

    public void stop() {
        this.started = false;
    }

    public void setParent(FileAppender<?> appender) {
        this.parent = appender;
    }

    public boolean isParentPrudent() {
        return this.parent.isPrudent();
    }

    public String getParentsRawFileProperty() {
        return this.parent.rawFileProperty();
    }
}
