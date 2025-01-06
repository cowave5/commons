/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.configuration;

import cn.hutool.system.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.Banner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * @author shanhuiming
 */
@Slf4j
public class CowaveBanner implements Banner {

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
        String appInfo = ":: Spring Boot 2.7.0 :: Commons 2.7.6 :: ";
        Resource resource = new DefaultResourceLoader().getResource("classpath:META-INF/git.info");
        if (resource.exists()) {
            try (InputStream input = resource.getInputStream()) {
                JSONObject json = JSON.parseObject(IOUtils.toString(input, StandardCharsets.UTF_8));
                String appName = (String) json.get("app.name");
                String appVersion = (String) json.get("app.version");
                String buildTime = (String) json.get("build.time");
                String gitId = (String) json.get("git.commit.id.abbrev");
                String gitBranch = (String) json.get("git.branch");
                if (StringUtils.isNotBlank(appName) && StringUtils.isNotBlank(gitBranch)) {
                    appInfo = appInfo + appName + " " + appVersion + " :: build(" + buildTime + " " + gitBranch + " " + gitId + ") ";
                } else if (StringUtils.isNotBlank(appName)) {
                    appInfo = appInfo + appName + " " + appVersion + " :: build(" + buildTime + ") ";
                } else if (StringUtils.isNotBlank(gitBranch)) {
                    appInfo = appInfo + "build(" + gitBranch + " " + gitId + ") ";
                }
            } catch (Exception e) {
                log.error("", e);
            }
        }

        OsInfo osInfo = SystemUtil.getOsInfo();
        HostInfo hostInfo = SystemUtil.getHostInfo();
        UserInfo userInfo = SystemUtil.getUserInfo();
        String userName = SystemUtil.get("user.name", false);
        JvmInfo jvmInfo = SystemUtil.getJvmInfo();
        JvmSpecInfo jvmSpecInfo = SystemUtil.getJvmSpecInfo();
        JavaInfo javaInfo = SystemUtil.getJavaInfo();
        JavaSpecInfo javaSpecInfo = SystemUtil.getJavaSpecInfo();
        JavaRuntimeInfo runtimeInfo = SystemUtil.getJavaRuntimeInfo();
        String bannerStr = "  ______    ______  ____    __    ____  ___   ____    ____  _______\n" +
                " /      |  /  __  \\ \\   \\  /  \\  /   / /   \\  \\   \\  /   / |   ____|\n" +
                "|  .----  |  |  |  | \\   \\/    \\/   / /  ^  \\  \\   \\/   /  |  |__\n" +
                "|  |      |  |  |  |  \\            / /  /_\\  \\  \\      /   |   __|\n" +
                "|  `----  |  `--'  |   \\    /\\    / /  _____  \\  \\    /    |  |____\n" +
                " \\______|  \\______/     \\__/  \\__/ /__/     \\__\\  \\__/     |_______|\n" +
                appInfo + "Copyright ©2017-06-30 Cowave All Rights Reserved\n" +
                ":: OS arch=" + osInfo.getArch() + " name=" + osInfo.getName() + " version=" + osInfo.getVersion() +
                " :: Host name=" + hostInfo.getName() + " addr=" + hostInfo.getAddress() +
                " :: User name=" + userName + " lang=" + userInfo.getLanguage() + "-" + userInfo.getCountry() + "\n" +

                ":: JVM " + jvmInfo.getName() + "(" + jvmInfo.getVersion() + " " + jvmInfo.getInfo() + ")" + " " + jvmInfo.getVendor() +
                " :: Spec " + jvmSpecInfo.getName() + " " + jvmSpecInfo.getVersion() + " " + jvmSpecInfo.getVendor() + "\n" +

                ":: Java " + javaInfo.getVersion() + " " + javaInfo.getVendor() + "(" + javaInfo.getVendorURL() + ")" +
                " :: Spec " + javaSpecInfo.getName() + " " + javaSpecInfo.getVersion() + " " + javaSpecInfo.getVendor() + "\n" +

                ":: Java Runtime " + runtimeInfo.getName() + " " + runtimeInfo.getVersion() +
                " :: Class " + runtimeInfo.getClassVersion() + " :: Home " + runtimeInfo.getHomeDir() + "\n" +
                ":: Java Library: " + runtimeInfo.getLibraryPath();
        out.println(bannerStr);
    }
}
