/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools.geospatial;

import lombok.Data;

/**
 * 屏幕像素坐标，以左上角为原点(0,0)
 *
 * @author shanhuiming
 */
@Data
class Pixel {

    /**
     * 横向像素
     */
    long x;

    /**
     * 纵向像素
     */
    long y;

    public Pixel(long x, long y){
        this.x=x;
        this.y=y;
    }
}
