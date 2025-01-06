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

import cn.hutool.core.util.CharsetUtil;
import com.cowave.commons.tools.ssh.output.LoggerOutputStream;
import com.jcraft.jsch.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * @author jiangbo
 */
@Slf4j
public class Terminal {

    /**
     * 执行bash命令
     */
    public static boolean execBash(String cmd) throws IOException, InterruptedException {
        String[] command = {"/bin/bash", "-c", cmd};
        return exec(command);
    }

    /**
     * 执行命令行
     */
    public static boolean exec(String[] cmd) throws IOException, InterruptedException {
        return exec(cmd, null, null);
    }

    /**
     * 执行命令行
     */
    public static boolean exec(String[] exec, Map<String, String> env, String workDir) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(exec);
        if (workDir != null) {
            processBuilder.directory(new File(workDir));
        }
        if (MapUtils.isNotEmpty(env)) {
            Map<String, String> pbEnv = processBuilder.environment();
            pbEnv.putAll(env);
        }

        Process process = processBuilder.start();
        return process.waitFor() == 0;
    }

    /**
     * 执行命令行，返回结果
     */
    public static Result process(String[] cmd, Map<String, String> env) {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        if (MapUtils.isNotEmpty(env)) {
            Map<String, String> pbEnv = processBuilder.environment();
            pbEnv.putAll(env);
        }

        try {
            Process p = processBuilder.start();
            String charset = getCharset(cmd);
            String output = IOUtils.toString(p.getInputStream(), charset);
            String error = IOUtils.toString(p.getErrorStream(), charset);
            int exitCode = p.waitFor();
            return new Result(exitCode == 0, exitCode, output, error);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Terminal runProcess interrupted");
            return new Result(false, -99, null, e.getMessage());
        } catch (Exception e) {
            log.error("Terminal runProcess failed", e);
            return new Result(false, -99, null, e.getMessage());
        }
    }

    /**
     * 执行Bash命令行，返回结果
     */
    public static Result processBash(String cmd) {
        String[] command = {"/bin/bash", "-c", cmd};
        return process(command, null);
    }

    /**
     * 执行Windows命令行，返回结果
     */
    public static Result processWindows(String cmd) {
        String[] command = {"cmd.exe", "/c", cmd};
        return process(command, null);
    }

    /**
     * 执行远程命令，返回结果
     */
    public static Result execRemote(String ip, Integer port, String username, String password, String command) {
        return execRemote(ip, port, username, password, command, 0, CharsetUtil.UTF_8);
    }

    /**
     * 执行远程命令，返回结果
     */
    public static Result execRemote(String ip, Integer port, String username, String password, String command, Integer timeout) {
        return execRemote(ip, port, username, password, command, timeout, CharsetUtil.UTF_8);
    }

    /**
     * 执行远程命令，返回结果
     */
    public static Result execRemote(String ip, Integer port, String username, String password, String command, Integer timeout, String charsetName) {
        try(LoggerOutputStream outputStream = new LoggerOutputStream(Charset.forName(charsetName));
            LoggerOutputStream extOutputStream = new LoggerOutputStream(Charset.forName(charsetName))) {
            return execRemote(ip, port, username, password, command, timeout, outputStream, extOutputStream);
        } catch (Exception e) {
            log.error("Terminal execRemote failed", e);
            return new Result(false, -99, null, e.getMessage());
        }
    }

    /**
     * 执行远程命令，返回结果
     */
    public static Result execRemote(String ip, Integer port, String username, String password, String command,
                              TerminalPipedOutputStream outputStream, TerminalPipedOutputStream extOutputStream) {
        return execRemote(ip, port, username, password, command, 0, outputStream, extOutputStream);
    }

    /**
     * 执行远程命令，返回结果
     */
    public static Result execRemote(String ip, Integer port, String username, String password, String command, Integer timeout,
                              TerminalPipedOutputStream outputStream, TerminalPipedOutputStream extOutputStream) {
        JSch jsch = new JSch();
        Session session = null;
        try {
            session = createSession(jsch, ip, port, username, password, timeout);
            return channelExec(session, command, timeout, outputStream, extOutputStream);
        } catch (Exception e) {
            log.error("Terminal execRemote failed", e);
            return new Result(false, -99, null, e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * 上传Remote文件
     */
    public static void sftpUpload(InputStream inputStream, String ip, Integer port, String username, String password, String remotePathFile) {
        JSch jsch = new JSch();
        Session session = null;
        try {
            session = createSession(jsch, ip, port, username, password, 0);
            channelSftp(session, inputStream, remotePathFile);
        } catch (Exception e) {
            throw new UnsupportedOperationException("复制文件失败", e);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private static Session createSession(JSch jsch, String ip, Integer port, String username, String password, int timeout) throws JSchException {
        Session session = jsch.getSession(username, ip, port);
        session.setPassword(password);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        session.setTimeout(timeout);
        return session;
    }

    private static Result channelExec(Session session, String command, Integer timeout,
                                      TerminalPipedOutputStream outputStream, TerminalPipedOutputStream extOutputStream) {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setOutputStream(outputStream);
            channel.setExtOutputStream(extOutputStream);
            channel.connect(Optional.ofNullable(timeout).orElse(0));
            String output = IOUtils.toString(outputStream.getInputStream(), StandardCharsets.UTF_8);
            String error = IOUtils.toString(extOutputStream.getInputStream(), StandardCharsets.UTF_8);
            int exitCode = channel.getExitStatus();
            return new Result(exitCode == 0, exitCode, output, error);
        } catch (Exception e) {
            log.error("Terminal execRemote failed", e);
            return Result.applyError(-99, null, e.getMessage());
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    private static void channelSftp(Session session, InputStream inputStream, String remotePathFile) throws SftpException, JSchException {
        ChannelSftp sftp = null;
        try {
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            String path = remotePathFile.substring(0, remotePathFile.lastIndexOf("/") + 1);
            createDir(sftp, path);
            sftp.setInputStream(inputStream);
            sftp.put(inputStream, remotePathFile);
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
        }
    }

    private static void createDir(ChannelSftp sftp, String remotePath) throws SftpException {
        if (isDirExist(sftp, remotePath)) {
            sftp.cd(remotePath);
            return;
        }

        String[] pathArray = remotePath.split("/");
        StringBuilder filePath = new StringBuilder("/");
        for (String path : pathArray) {
            if ("".equals(path)) {
                continue;
            }

            filePath.append(path).append("/");
            if (isDirExist(sftp, filePath.toString())) {
                sftp.cd(filePath.toString());
            } else {
                sftp.mkdir(filePath.toString());
                sftp.cd(filePath.toString());
            }
        }
        sftp.cd(remotePath);
    }

    private static boolean isDirExist(ChannelSftp sftp, String remotePath) throws SftpException {
        try {
            SftpATTRS sftpAttrs = sftp.lstat(remotePath);
            if(sftpAttrs.isDir()){
                return true;
            }else{
                throw new RuntimeException(remotePath + " is exist, but isn't a dir");
            }
        } catch (SftpException e) {
            if (e.getMessage().equalsIgnoreCase("no such file")) {
                return false;
            }
            throw e;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {

        boolean success;

        private int exitCode;

        private String output;

        private String error;

        public static Result applySuccess(String output) {
            return new Result(true, 0, output, null);
        }

        public static Result applyError(int exitCode, String output, String error) {
            return new Result(false, exitCode, output, error);
        }
    }

    private static String getCharset(String[] exec) {
        if (exec == null || exec.length == 0) {
            return CharsetUtil.UTF_8;
        }
        if ("cmd.exe".equals(exec[0])) {
            return CharsetUtil.GBK;
        }
        return CharsetUtil.UTF_8;
    }
}
