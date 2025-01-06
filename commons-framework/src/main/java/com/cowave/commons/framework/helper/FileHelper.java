/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletResponse;

import com.cowave.commons.client.http.asserts.AssertsException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author shanhuiming
 *
 * <p>spring.servlet.multipart.location: ${java.io.tmpdir} # 临时目录
 * <p>spring.servlet.multipart.max-file-size: 20MB         # 单个文件最大为20M
 * <p>spring.servlet.multipart.max-request-size: 20MB      # 单次请求文件总大小为20M
 */
@Component
public class FileHelper {

    // 图片、office、压缩文件、视频、pdf
    @Value("#{'${spring.servlet.multipart.limits:bmp,gif,jpg,jpeg,png,doc,docx,xls,xlsx,ppt,pptx,html,htm,txt,rar,zip,gz,bz2,mp4,avi,rmvb,pdf,blob}'.split(',')}")
    private String[] fileLimits;

    private boolean suffixValid(String suffix) {
        suffix = suffix.toLowerCase();
        for (String str : fileLimits) {
            if (str.equals(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 上传文件
     *
     * @param multipartFile 上传文件
     * @param dirPath       本地目录
     */
    public String upload(MultipartFile multipartFile, String dirPath) throws Exception {
        String name = multipartFile.getOriginalFilename();
        String suffix = FilenameUtils.getExtension(name);
        if (!suffixValid(suffix)) {
            throw new AssertsException("{frame.file.invalid}", name);
        }

        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new AssertsException("{frame.dir.failed}", dirPath);
        }

        String md5 = DigestUtils.md5Hex(multipartFile.getBytes());
        String filePath = dirPath + File.separator + name + "." + md5;
        multipartFile.transferTo(Paths.get(filePath));
        return filePath;
    }

    /**
     * 下载文件
     *
     * @param filename 文件名称
     * @param filePath 本地路径
     */
    public void download(HttpServletResponse resp, String filename, String filePath) throws IOException {
        filename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("\\+", "%20");
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/octet-stream;charset=UTF-8");
        resp.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename + "\"");
        try (FileInputStream fileInput = new FileInputStream(filePath);
             OutputStream outPut = resp.getOutputStream();) {
            IOUtils.copy(fileInput, outPut);
        }
    }
}
