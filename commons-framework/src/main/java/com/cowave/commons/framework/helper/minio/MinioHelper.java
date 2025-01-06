/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.minio;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletResponse;

import io.minio.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.multipart.MultipartFile;

import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author shanhuiming
 *
 */
@ConditionalOnClass(MinioClient.class)
@RequiredArgsConstructor
public class MinioHelper {

    private final MinioClient minioClient;

    private static final String POLICY_PUBLIC = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetBucketLocation\",\"s3:ListBucket\",\"s3:ListBucketMultipartUploads\"],\"Resource\":[\"arn:aws:s3:::%s\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}";

    /**
     * 创建存储桶
     *
     * @param bucket   存储桶名称
     * @param isPublic 是否公开的
     */
    public void makeBucket(String bucket, boolean isPublic) throws Exception{
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        if(isPublic){
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket)
                    .config(String.format(POLICY_PUBLIC, bucket, bucket)).build());
        }
    }

    /**
     * 删除存储桶
     *
     * @param bucket 存储桶名称
     */
    public void deleteBucket(String bucket) throws Exception{
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
    }

    /**
     * 是否存在存储桶
     *
     * @param bucket 存储桶名称
     */
    public boolean existBucket(String bucket) throws Exception{
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
    }

    /**
     * 存储桶列表
     */
    public List<Bucket> listBuckets() throws Exception{
        return minioClient.listBuckets();
    }

    /**
     * 列举存储桶
     *
     * @param bucket 存储桶名称
     */
    public List<Item> listItems(String bucket) throws Exception{
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).build());
        List<Item> items = new ArrayList<>();
        for (Result<Item> result : results) {
            items.add(result.get());
        }
        return items;
    }

    /**
     * 上传
     *
     * @param multipartFile 文件
     * @param bucket   存储桶
     * @param path     路径
     * @param isPublic 是否公开的
     */
    public void upload(MultipartFile multipartFile, String bucket, String path, boolean isPublic) throws Exception{
        if(!existBucket(bucket)) {
            makeBucket(bucket, isPublic);
        }
        PutObjectArgs objectArgs = PutObjectArgs.builder().bucket(bucket).object(path)
                .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
                .contentType(multipartFile.getContentType()).build();
        minioClient.putObject(objectArgs);
    }

    /**
     * 上传
     *
     * @param inputStream 输入流
     * @param bucket   存储桶
     * @param path     路径
     * @param isPublic 是否公开的
     */
    public void upload(InputStream inputStream, String bucket, String path, boolean isPublic) throws Exception {
        if (!existBucket(bucket)) {
            makeBucket(bucket, isPublic);
        }
        minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(path)
                .stream(inputStream, inputStream.available(), -1).build());
    }

    /**
     * 获取InputStream
     *
     * @param bucket 存储桶
     * @param path   路径
     */
    public InputStream getInputStream(String bucket, String path) throws Exception {
        GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(bucket).object(path).build();
        return minioClient.getObject(objectArgs);
    }

    /**
     * 下载
     *
     * @param bucket 存储桶
     * @param path   路径
     * @param name   下载名称
     */
    public void download(HttpServletResponse resp, String bucket, String path, String name) throws Exception{
        name = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("\\+", "%20");
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/octet-stream;charset=UTF-8");
        resp.setHeader("Content-disposition", "attachment;filename*=utf-8''" + name + "\"");
        GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(bucket).object(path).build();
        try (GetObjectResponse response = minioClient.getObject(objectArgs);
                OutputStream outPut = resp.getOutputStream()){
            IOUtils.copy(response, outPut);
        }
    }

    /**
     * 下载压缩包
     *
     * @param bucket  存储桶
     * @param pathMap entry名称与路径映射
     * @param name    压缩包名称
     */
    public void archiveDownload(HttpServletResponse resp, String bucket, Map<String, String> pathMap, String name) throws Exception {
        name = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("\\+", "%20");
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/octet-stream;charset=UTF-8");
        resp.setHeader("Content-disposition", "attachment;filename*=utf-8''" + name + "\"");
        archiveToOutputStream(resp.getOutputStream(), bucket, pathMap);
    }

    /**
     * 压缩到输出流
     *
     * @param outputStream 输出流
     * @param bucket  存储桶
     * @param pathMap 名称与路径映射
     */
    public void archiveToOutputStream(OutputStream outputStream, String bucket, Map<String, String> pathMap) throws Exception {
        try (TarArchiveOutputStream archive = new TarArchiveOutputStream(new GZIPOutputStream(outputStream))) {
            archive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (Map.Entry<String, String> entry : pathMap.entrySet()) {
                try (GetObjectResponse response = minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucket).object(entry.getValue()).build())) {
                    TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
                    tarEntry.setSize(Long.parseLong(Objects.requireNonNull(response.headers().get("Content-Length"))));
                    archive.putArchiveEntry(tarEntry);
                    IOUtils.copy(response, archive);
                    archive.closeArchiveEntry();
                }
            }
            archive.finish();
        }
    }

    /**
     * 预览
     *
     * @param bucket 存储桶
     * @param path   路径
     */
    public String preview(String bucket, String path) throws Exception{
        GetPresignedObjectUrlArgs build = GetPresignedObjectUrlArgs
                .builder().bucket(bucket).object(path).method(Method.GET).build();
        return minioClient.getPresignedObjectUrl(build);
    }

    /**
     * 预览
     *
     * @param bucket 存储桶
     * @param path   路径
     * @param expireSeconds 超时
     */
    public String preview(String bucket, String path, int expireSeconds) throws Exception{
        GetPresignedObjectUrlArgs build = GetPresignedObjectUrlArgs
                .builder().bucket(bucket).object(path).method(Method.GET).expiry(expireSeconds).build();
        return minioClient.getPresignedObjectUrl(build);
    }

    /**
     * 删除
     *
     * @param bucket 存储桶
     * @param path   路径
     */
    public void delete(String bucket, String path) throws Exception{
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(path).build());
    }
}
