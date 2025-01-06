/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.ssh.output;

import com.cowave.commons.tools.ssh.TerminalPipedOutputStream;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author jiangbo
 */
@Slf4j
public class LoggerOutputStream extends TerminalPipedOutputStream {

    public LoggerOutputStream(Charset charsetName) throws IOException {
        super(charsetName);
    }

    @Override
    public void handlerLine(String line) {
        log.info(line);
    }

    public static LoggerOutputStream apply() throws IOException {
        return apply(StandardCharsets.UTF_8);
    }

    public static LoggerOutputStream apply(Charset charsetName) throws IOException {
        return new LoggerOutputStream(charsetName);
    }
}
