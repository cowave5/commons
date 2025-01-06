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

import java.util.List;

import lombok.Data;

/**
 *
 * @author shanhuiming
 *
 */
@Data
public class GeoArea {

    /**
     * 圆形
     */
    public static final int SHAPE_CIRCLE = 0;

    /**
     * 多边形
     */
    public static final int SHAPE_POLYGON = 1;

    /**
     * 轨迹
     */
    public static final int SHAPE_TRACK = 2;

    /**
     * 区域形状 0：圆形 1:多边形 2：航迹
     */
    private Integer type;

    /**
     * 区域点集合
     */
    private List<GeoPoint> points;
}
