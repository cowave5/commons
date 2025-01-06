/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.ssh;

import lombok.Getter;

import java.io.IOException;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @author jiangbo
 */
@Getter
public abstract class TerminalPipedOutputStream extends PipedOutputStream {

    protected final Charset charsetName;

    private final TerminalPipedInputStream inputStream;

    protected int count;

    protected byte[] buffer = new byte[1024];

    public TerminalPipedOutputStream(Charset charsetName) throws IOException{
        this(new TerminalPipedInputStream(32 * 1024), charsetName);
    }

    public TerminalPipedOutputStream(TerminalPipedInputStream input, Charset charsetName) throws IOException{
        super(input);
        this.inputStream = input;
        this.charsetName = charsetName;
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\n') {
            writeLog();
        } else {
            if (count == buffer.length - 1) {
                byte[] newArray = new byte[buffer.length * 2];
                System.arraycopy(buffer, 0, newArray, 0, buffer.length);
                buffer = newArray;
            }
            buffer[count++] = (byte) b;
        }
        if(inputStream != null) {
            inputStream.checkSpace(1);
        }
        super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }
    }

    @Override
    public void close() throws IOException {
        if (count > 0) {
            writeLog();
        }
        super.close();
    }

    protected synchronized void writeLog() {
        byte[] bytes = Arrays.copyOf(buffer, count);
        handlerLine(new String(bytes, charsetName));
        buffer = new byte[1024];
        count = 0;
    }

    /**
     * 逐行处理输出
     */
    public abstract void handlerLine(String line);
}
