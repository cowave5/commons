/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.framework.helper.http.chooser;

import com.cowave.commons.client.http.HttpServiceChooser;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
public class DefaultServiceChooser implements HttpServiceChooser {

    private final EurekaServiceChooser eurekaServiceChooser;

    private final NacosServiceChooser nacosServiceChooser;

    @Override
    public String choose(String name) {
        String serviceUrl = null;
        if(eurekaServiceChooser != null){
            serviceUrl = eurekaServiceChooser.choose(name);
        }
        if(serviceUrl == null && nacosServiceChooser != null){
            serviceUrl = nacosServiceChooser.choose(name);
        }
        return serviceUrl;
    }
}
