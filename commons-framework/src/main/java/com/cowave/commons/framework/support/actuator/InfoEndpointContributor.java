/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.support.actuator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author shanhuiming
 *
 */
@Slf4j
@Component
public class InfoEndpointContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        Resource resource = new DefaultResourceLoader().getResource("classpath:META-INF/git.info");
        if (resource.exists()) {
            try (InputStream input = resource.getInputStream()) {
                JSONObject json = JSON.parseObject(IOUtils.toString(input, StandardCharsets.UTF_8));
                if(json.get("app.name") != null){
                    Map<String, Object> application = new LinkedHashMap<>();
                    application.put("name", json.get("app.name"));
                    application.put("version", json.get("app.version"));
                    application.put("build", json.get("build.time"));
                    builder.withDetail("application", application);
                }
                if(json.get("git.branch") != null){
                    Map<String, Object> commit = new LinkedHashMap<>();
                    commit.put("commit-id", json.get("git.branch") + " " + json.get("git.commit.id.abbrev"));
                    commit.put("commit-msg", json.get("git.commit.message.short"));
                    commit.put("Author", json.get("git.commit.user.email"));
                    commit.put("Time", json.get("git.commit.time"));
                    builder.withDetail("commit", commit);
                }
            }catch (Exception e){
                log.error("", e);
            }
        }
    }
}
