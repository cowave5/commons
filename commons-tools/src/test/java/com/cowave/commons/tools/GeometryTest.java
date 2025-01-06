/*
 * Copyright (c) 2017～2025 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.tools;

import com.alibaba.fastjson.JSON;
import com.cowave.commons.tools.geospatial.GeoArea;
import com.cowave.commons.tools.geospatial.GeoPoint;
import com.cowave.commons.tools.geospatial.GeometryHelper;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author shanhuiming
 *
 */
public class GeometryTest {

    /**
     * 多边形区域1
     * {longitude: 104.9707, latitude: 33.7528}
     * {longitude: 112.002 , latitude: 30.0226}
     * {longitude: 113.7012, latitude: 36.1063}
     * {longitude: 108.9551, latitude: 37.9769}
     * {longitude: 104.9707, latitude: 33.7528}
     * <p>
     * 多边形区域2
     * {longitude: 110.5957, latitude: 36.9069}
     * {longitude: 105.9082, latitude: 33.996 }
     * {longitude: 110.9473, latitude: 32.1301}
     * {longitude: 113.1152, latitude: 35.393 }
     * {longitude: 110.5957, latitude: 36.9069}
     * <p>
     * 多边形区域1完全包含多边形区域2
     */
    @Test
    public void test1() {
        List<GeoArea> list1 = new ArrayList<>();
        GeoArea geoArea = new GeoArea();
        geoArea.setType(1);
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(104.9707, 33.7528));
        points.add(new GeoPoint(112.002, 30.0226));
        points.add(new GeoPoint(113.7012, 36.1063));
        points.add(new GeoPoint(108.9551, 37.9769));
        points.add(new GeoPoint(104.9707, 33.7528));
        geoArea.setPoints(points);
        list1.add(geoArea);

        List<GeoArea> list2 = new ArrayList<>();
        GeoArea geoArea2 = new GeoArea();
        geoArea2.setType(1);
        List<GeoPoint> points2 = new ArrayList<>();
        points2.add(new GeoPoint(110.5957, 36.9069));
        points2.add(new GeoPoint(105.9082, 33.996));
        points2.add(new GeoPoint(110.9473, 32.1301));
        points2.add(new GeoPoint(113.1152, 35.393));
        points2.add(new GeoPoint(110.5957, 36.9069));
        geoArea2.setPoints(points2);
        list2.add(geoArea2);

        //通过任务区域生成Geometry地理空间对象
        Geometry geometry1 = GeometryHelper.unionArea(list1);
        Geometry geometry2 = GeometryHelper.unionArea(list2);
        assertTrue(geometry1.contains(geometry2));
    }

    /**
     * 多边形区域1
     * {longitude: 111.8848, latitude: 38.5749}
     * {longitude: 105.4395, latitude: 36.6253}
     * {longitude: 104.209,  latitude: 32.5756}
     * {longitude: 110.8301, latitude: 27.7138}
     * {longitude: 116.8066, latitude: 34.8178}
     * {longitude: 111.8848, latitude: 38.5749}
     * <p>
     * 多边形区域2
     * {longitude: 104.9707, latitude: 33.7528}
     * {longitude: 112.002 , latitude: 30.0226}
     * {longitude: 113.7012, latitude: 36.1063}
     * {longitude: 108.9551, latitude: 37.9769}
     * {longitude: 104.9707, latitude: 33.7528}
     * <p>
     * 多边形区域1不完全包含多边形区域2
     */
    @Test
    public void test2() {
        List<GeoArea> list1 = new ArrayList<>();
        GeoArea geoArea = new GeoArea();
        geoArea.setType(1);
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(111.8848, 38.5749));
        points.add(new GeoPoint(105.4395, 36.6253));
        points.add(new GeoPoint(104.209, 32.5756));
        points.add(new GeoPoint(110.8301, 27.7138));
        points.add(new GeoPoint(116.8066, 34.8178));
        points.add(new GeoPoint(111.8848, 38.5749));
        geoArea.setPoints(points);
        list1.add(geoArea);

        List<GeoArea> list2 = new ArrayList<>();
        GeoArea geoArea2 = new GeoArea();
        geoArea2.setType(1);
        List<GeoPoint> points2 = new ArrayList<>();
        points2.add(new GeoPoint(104.9707, 33.7528));
        points2.add(new GeoPoint(112.002, 30.0226));
        points2.add(new GeoPoint(113.7012, 36.1063));
        points2.add(new GeoPoint(108.9551, 37.9769));
        points2.add(new GeoPoint(104.9707, 33.7528));
        geoArea2.setPoints(points2);
        list2.add(geoArea2);

        //通过任务区域生成Geometry地理空间对象
        Geometry geometry1 = GeometryHelper.unionArea(list1);
        Geometry geometry2 = GeometryHelper.unionArea(list2);
        assertFalse(geometry1.contains(geometry2));
    }

    /**
     * 多边形区域1
     * {longitude: 111.8848, latitude: 38.5749}
     * {longitude: 105.4395, latitude: 36.6253}
     * {longitude: 104.209,  latitude: 32.5756}
     * {longitude: 110.8301, latitude: 27.7138}
     * {longitude: 116.8066, latitude: 34.8178}
     * {longitude: 111.8848, latitude: 38.5749}
     * <p>
     * 多边形区域2
     * {longitude: 110.5957, latitude: 36.9069}
     * {longitude: 105.9082, latitude: 33.996 }
     * {longitude: 110.9473, latitude: 32.1301}
     * {longitude: 113.1152, latitude: 35.393 }
     * {longitude: 110.5957, latitude: 36.9069}
     * <p>
     * 多边形区域1完全包含多边形区域2
     */
    @Test
    public void test3() {
        List<GeoArea> list1 = new ArrayList<>();
        GeoArea geoArea = new GeoArea();
        geoArea.setType(1);
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(111.8848, 38.5749));
        points.add(new GeoPoint(105.4395, 36.6253));
        points.add(new GeoPoint(104.209, 32.5756));
        points.add(new GeoPoint(110.8301, 27.7138));
        points.add(new GeoPoint(116.8066, 34.8178));
        points.add(new GeoPoint(111.8848, 38.5749));
        geoArea.setPoints(points);
        list1.add(geoArea);

        List<GeoArea> list2 = new ArrayList<>();
        GeoArea geoArea2 = new GeoArea();
        geoArea2.setType(1);
        List<GeoPoint> points2 = new ArrayList<>();
        points2.add(new GeoPoint(110.5957, 36.9069));
        points2.add(new GeoPoint(105.9082, 33.996));
        points2.add(new GeoPoint(110.9473, 32.1301));
        points2.add(new GeoPoint(113.1152, 35.393));
        points2.add(new GeoPoint(110.5957, 36.9069));
        geoArea2.setPoints(points2);
        list2.add(geoArea2);

        //通过任务区域生成Geometry地理空间对象
        Geometry geometry1 = GeometryHelper.unionArea(list1);
        Geometry geometry2 = GeometryHelper.unionArea(list2);
        assertTrue(geometry1.contains(geometry2));
    }

    /**
     * 多边形区域1
     * 纬度         经度
     * 36.3898    109.541
     * 29.7177    104.3262
     * 25.0369    110.0098
     * 26.9329    117.3926
     * 33.0189    116.4551
     * 36.3898    109.541
     * 圆形区域2
     * 圆心 经纬度  31.3828,110.0684
     * 半径 382839.6938米
     * 区域1完全包含区域2
     */
    @Test
    public void test4() {
        List<GeoArea> list1 = new ArrayList<>();
        GeoArea geoArea = new GeoArea();
        geoArea.setType(1);
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(109.541, 36.3898));
        points.add(new GeoPoint(104.3262, 29.7177));
        points.add(new GeoPoint(110.0098, 25.0369));
        points.add(new GeoPoint(117.3926, 26.9329));
        points.add(new GeoPoint(116.4551, 33.0189));
        points.add(new GeoPoint(109.541, 36.3898));
        geoArea.setPoints(points);
        list1.add(geoArea);

        List<GeoArea> list2 = new ArrayList<>();
        GeoArea geoArea2 = new GeoArea();
        geoArea2.setType(0);
        List<GeoPoint> points2 = new ArrayList<>();
        GeoPoint geoPoint = new GeoPoint(110.0684, 31.3828);
        geoPoint.setRadius(382839.6938);
        points2.add(geoPoint);
        geoArea2.setPoints(points2);
        list2.add(geoArea2);

        //通过任务区域生成Geometry地理空间对象
        Geometry geometry1 = GeometryHelper.unionArea(list1);
        Geometry geometry2 = GeometryHelper.unionArea(list2);
        assertTrue(geometry1.contains(geometry2));
    }

    /**
     * 多边形区域1
     * 纬度         经度
     * 36.3898    109.541
     * 29.7177    104.3262
     * 25.0369    110.0098
     * 26.9329    117.3926
     * 33.0189    116.4551
     * 36.3898    109.541
     * 圆形区域2
     * 圆心 经纬度  29.7686,113.877
     * 半径 353728.8382米
     * 区域1不能完全包含区域2
     */
    @Test
    public void test5() {
        List<GeoArea> list1 = new ArrayList<>();
        GeoArea geoArea = new GeoArea();
        geoArea.setType(1);
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(109.541, 36.3898));
        points.add(new GeoPoint(104.3262, 29.7177));
        points.add(new GeoPoint(110.0098, 25.0369));
        points.add(new GeoPoint(117.3926, 26.9329));
        points.add(new GeoPoint(116.4551, 33.0189));
        points.add(new GeoPoint(109.541, 36.3898));
        geoArea.setPoints(points);
        list1.add(geoArea);

        List<GeoArea> list2 = new ArrayList<>();
        GeoArea geoArea2 = new GeoArea();
        geoArea2.setType(GeoArea.SHAPE_CIRCLE);

        List<GeoPoint> points2 = new ArrayList<>();
        GeoPoint geoPoint = new GeoPoint(113.877, 29.7686);
        geoPoint.setRadius(353728.8382);
        points2.add(geoPoint);
        geoArea2.setPoints(points2);
        list2.add(geoArea2);

        //通过任务区域生成Geometry地理空间对象
        Geometry geometry1 = GeometryHelper.unionArea(list1);
        Geometry geometry2 = GeometryHelper.unionArea(list2);
        assertFalse(geometry1.contains(geometry2));
    }

    /**
     * 多边形区域1
     * {longitude: 111.8848, latitude: 38.5749}
     * {longitude: 105.4395, latitude: 36.6253}
     * {longitude: 104.209,  latitude: 32.5756}
     * {longitude: 110.8301, latitude: 27.7138}
     * {longitude: 116.8066, latitude: 34.8178}
     * {longitude: 111.8848, latitude: 38.5749}
     * <p>
     * 多边形区域2
     * {longitude: 110.5957, latitude: 36.9069}
     * {longitude: 105.9082, latitude: 33.996 }
     * {longitude: 110.9473, latitude: 32.1301}
     * {longitude: 113.1152, latitude: 35.393 }
     * {longitude: 110.5957, latitude: 36.9069}
     * <p>
     * 多边形区域1完全包含多边形区域2
     */
    @Test
    public void test6() {
        String coverage =
                "[{\"longitude\": 11188480, \"latitude\": 3857490,\"groupID\": 0}," +
                "{\"longitude\": 10543950, \"latitude\": 3662530,\"groupID\": 0}," +
                "{\"longitude\": 10420900, \"latitude\": 3257560,\"groupID\": 0}," +
                "{\"longitude\": 11083010, \"latitude\": 2771380,\"groupID\": 0}," +
                "{\"longitude\": 11680660, \"latitude\": 3481780,\"groupID\": 0}]";

        List<GeoArea> list2 = new ArrayList<>();
        GeoArea geoArea2 = new GeoArea();
        geoArea2.setType(GeoArea.SHAPE_POLYGON);
        List<GeoPoint> points2 = new ArrayList<>();
        points2.add(new GeoPoint(110.5957, 36.9069));
        points2.add(new GeoPoint(105.9082, 33.996));
        points2.add(new GeoPoint(110.9473, 32.1301));
        points2.add(new GeoPoint(113.1152, 35.393));
        points2.add(new GeoPoint(110.5957, 36.9069));
        geoArea2.setPoints(points2);
        list2.add(geoArea2);

        //通过任务区域生成Geometry地理空间对象
        Geometry geometry1 = GeometryHelper.createPolygon(JSON.parseArray(coverage, GeoPoint.class), true);
        Geometry geometry2 = GeometryHelper.unionArea(list2);
        assert geometry1 != null;
        assertTrue(geometry1.contains(geometry2));
    }
}
