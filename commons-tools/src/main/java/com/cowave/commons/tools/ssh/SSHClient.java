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

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Properties;

/**
 *
 * @author cailinfei
 *
 */
@Slf4j
public class SSHClient implements AutoCloseable {

    private final String host;

    private int port = 22;

    private final String user;

    private final String passwd;

    private final JSch jsch = new JSch();

    private Session session;

    public SSHClient(String host, String user, String passwd) throws JSchException {
        this.host = host;
        this.user = user;
        this.passwd = passwd;

        Properties properties = new Properties();
        properties.put("StrictHostKeyChecking", "no");
        session.setConfig(properties);
        session = jsch.getSession(user, host, port);
        session.setPassword(passwd);
    }

    public SSHClient(String host, int port, String user, String passwd) throws JSchException {
        this.host = host;
        this.port = port;
        this.user = user;
        this.passwd = passwd;

        Properties properties = new Properties();
        properties.put("StrictHostKeyChecking", "no");
        session.setConfig(properties);
        session = jsch.getSession(user, host, port);
        session.setPassword(passwd);
    }

    @Override
    public void close() {
        if(session != null){
            session.disconnect();
        }
    }

    /**
     * 执行命令
     */
    public int exec(String cmd) throws Exception {
        ChannelExec execChannel = (ChannelExec)session.openChannel("exec");
        execChannel.setCommand(cmd);
        execChannel.setInputStream(null);
        execChannel.setErrStream(System.err);
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(execChannel.getInputStream()))){
            execChannel.connect();
            String buf;
            while ((buf = reader.readLine()) != null) {
                log.info(buf);
            }
            if (execChannel.isClosed()) {
                int returnCode = execChannel.getExitStatus();
                log.info("Exit-status:" + returnCode);
                return returnCode;
            }
        }finally {
            execChannel.disconnect();
        }
        return -1;
    }


    /**
     * 上传
     */
    public void sftpUpload(InputStream inputStream, String remotePath) throws Exception {
        ChannelSftp sftpChannel = (ChannelSftp)session.openChannel("sftp");
        sftpChannel.connect();
        try {
            String remoteDir = remotePath.substring(0, remotePath.lastIndexOf("/") + 1);
            if (!isDirExist(sftpChannel, remoteDir)) {
                createDir(sftpChannel, remoteDir);
            }
            sftpChannel.setInputStream(inputStream);
            sftpChannel.put(inputStream, remotePath);
        } finally {
            sftpChannel.disconnect();
        }
    }

    /**
     * 下载
     */
    public void sftpDownload(String remote, String local) throws Exception {
        ChannelSftp sftpChannel = (ChannelSftp)session.openChannel("sftp");
        try(OutputStream output = new FileOutputStream(local)){
            sftpChannel.connect(5000);
            sftpChannel.get(remote, output);
            output.flush();
        }finally {
            sftpChannel.disconnect();
        }
    }

    private boolean isDirExist(ChannelSftp sftp, String remotePath) throws SftpException {
        try {
            SftpATTRS sftpAttrs = sftp.lstat(remotePath);
            if(sftpAttrs.isDir()){
                return true;
            }else{
                throw new RuntimeException(remotePath + " is exist, but not a dir");
            }
        } catch (SftpException e) {
            if (e.getMessage().equalsIgnoreCase("no such file")) {
                return false;
            }
            throw e;
        }
    }

    private void createDir(ChannelSftp sftp, String remotePath) throws SftpException {
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
}
