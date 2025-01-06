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
import ch.qos.logback.core.rolling.helper.ArchiveRemover;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.FileFilterUtil;
import ch.qos.logback.core.rolling.helper.SizeAndTimeBasedArchiveRemover;
import ch.qos.logback.core.util.DefaultInvocationGate;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.InvocationGate;

import java.io.File;
import java.util.Date;

/**
 * @author shanhuiming
 * @see ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP
 */
@NoAutoStart
public class SizeAndTimeBasedFNATP<E> extends TimeBasedFileNamingAndTriggeringPolicyBase<E> {
    int currentPeriodsCounter;
    FileSize maxFileSize;
    long nextSizeCheck;
    static String MISSING_INT_TOKEN = "Missing integer token, that is %i, in FileNamePattern [";
    static String MISSING_DATE_TOKEN = "Missing date token, that is %d, in FileNamePattern [";
    private final Usage usage;
    InvocationGate invocationGate;

    public SizeAndTimeBasedFNATP() {
        this(Usage.DIRECT);
    }

    public SizeAndTimeBasedFNATP(Usage usage) {
        this.currentPeriodsCounter = 0;
        this.nextSizeCheck = 0L;
        this.invocationGate = new DefaultInvocationGate();
        this.usage = usage;
    }

    public void start() {
        super.start();
        if (this.usage == Usage.DIRECT) {
            this.addWarn("SizeAndTimeBasedFNATP is deprecated. Use SizeAndTimeBasedRollingPolicy instead");
            this.addWarn("For more information see http://logback.qos.ch/manual/appenders.html#SizeAndTimeBasedRollingPolicy");
        }

        if (super.isErrorFree()) {
            if (this.maxFileSize == null) {
                this.addError("maxFileSize property is mandatory.");
                this.withErrors();
            }

            if (!this.validateDateAndIntegerTokens()) {
                this.withErrors();
            } else {
                this.archiveRemover = this.createArchiveRemover();
                this.archiveRemover.setContext(this.context);
                String regex = this.tbrp.fileNamePattern.toRegexForFixedDate(this.dateInCurrentPeriod);
                String stemRegex = FileFilterUtil.afterLastSlash(regex);
                this.computeCurrentPeriodsHighestCounterValue(stemRegex);
                if (this.isErrorFree()) {
                    this.started = true;
                }

            }
        }
    }

    private boolean validateDateAndIntegerTokens() {
        boolean inError = false;
        if (this.tbrp.fileNamePattern.getIntegerTokenConverter() == null) {
            inError = true;
            this.addError(MISSING_INT_TOKEN + this.tbrp.fileNamePatternStr + "]");
            this.addError("See also http://logback.qos.ch/codes.html#sat_missing_integer_token");
        }

        if (this.tbrp.fileNamePattern.getPrimaryDateTokenConverter() == null) {
            inError = true;
            this.addError(MISSING_DATE_TOKEN + this.tbrp.fileNamePatternStr + "]");
        }

        return !inError;
    }

    protected ArchiveRemover createArchiveRemover() {
        return new SizeAndTimeBasedArchiveRemover(this.tbrp.fileNamePattern, this.rc);
    }

    void computeCurrentPeriodsHighestCounterValue(String stemRegex) {
        File file = new File(this.getCurrentPeriodsFileNameWithoutCompressionSuffix());
        File parentDir = file.getParentFile();
        File[] matchingFileArray = FileFilterUtil.filesInFolderMatchingStemRegex(parentDir, stemRegex);
        if (matchingFileArray != null && matchingFileArray.length != 0) {
            this.currentPeriodsCounter = FileFilterUtil.findHighestCounter(matchingFileArray, stemRegex);
            if (this.tbrp.getParentsRawFileProperty() != null || this.tbrp.compressionMode != CompressionMode.NONE) {
                ++this.currentPeriodsCounter;
            }

        } else {
            this.currentPeriodsCounter = 0;
        }
    }

    public boolean isTriggeringEvent(File activeFile, E event) {
        long time = this.getCurrentTime();
//        去掉时间滚动，避免日志文件过多
//        if (time >= this.nextCheck) {
//            Date dateInElapsedPeriod = this.dateInCurrentPeriod;
//            this.elapsedPeriodsFileName = this.tbrp.fileNamePatternWithoutCompSuffix.convertMultipleArguments(new Object[]{dateInElapsedPeriod, this.currentPeriodsCounter});
//            this.currentPeriodsCounter = 0;
//            this.setDateInCurrentPeriod(time);
//            this.computeNextCheck();
//            return true;
//        } else if (this.invocationGate.isTooSoon(time)) {
//            return false;
//        } else

        if (activeFile == null) {
            this.addWarn("activeFile == null");
            return false;
        } else if (this.maxFileSize == null) {
            this.addWarn("maxFileSize = null");
            return false;
        } else if (activeFile.length() >= this.maxFileSize.getSize()) {
            if (time >= this.nextCheck) {
                Date dateInElapsedPeriod = this.dateInCurrentPeriod;
                this.elapsedPeriodsFileName = this.tbrp.fileNamePatternWithoutCompSuffix.convertMultipleArguments(new Object[]{dateInElapsedPeriod, this.currentPeriodsCounter});
                this.currentPeriodsCounter = 0;
                this.setDateInCurrentPeriod(time);
                this.computeNextCheck();
            }else{
                this.elapsedPeriodsFileName = this.tbrp.fileNamePatternWithoutCompSuffix.convertMultipleArguments(new Object[]{this.dateInCurrentPeriod, this.currentPeriodsCounter});
                ++this.currentPeriodsCounter;
            }
            return true;
        } else {
            return false;
        }
    }

    public String getCurrentPeriodsFileNameWithoutCompressionSuffix() {
        return this.tbrp.fileNamePatternWithoutCompSuffix.convertMultipleArguments(new Object[]{this.dateInCurrentPeriod, this.currentPeriodsCounter});
    }

    public void setMaxFileSize(FileSize aMaxFileSize) {
        this.maxFileSize = aMaxFileSize;
    }

    static enum Usage {
        EMBEDDED,
        DIRECT;

        private Usage() {
        }
    }
}
