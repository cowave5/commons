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

import cn.hutool.core.lang.Assert;
import com.vividsolutions.jts.geom.Coordinate;
import org.apache.commons.collections4.CollectionUtils;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author shanhuiming
 *
 */
public class GeometryHelper {

    /**
     * WGS84坐标系
     */
    private static final int COORDINATE_SYSTEM_CODE = 4326;

    /**
     * 圆近似为多边形时的边数,边数越大越接近圆，同时运算量也越大
     */
    private static final int NUMBER_EDGES_OF_POLYGON = 144;

    /**
     * 设置精度
     */
    private static final PrecisionModel PRECISION_MODEL = new PrecisionModel(PrecisionModel.FLOATING);

    /**
     * 设置坐标系
     */
    private static final org.locationtech.jts.geom.GeometryFactory GEOMETRY_FACTORY = new org.locationtech.jts.geom.GeometryFactory(PRECISION_MODEL, COORDINATE_SYSTEM_CODE);

    /**
     * 联合区域
     *
     * @param list 区域描述
     */
    public static Geometry unionArea(List<GeoArea> list) {
        Geometry union = null;
        for (GeoArea geoArea : list) {
            Geometry geometry = createGeometry(geoArea);
            union = union == null ? geometry : union.union(geometry);
        }
        return union;
    }

    /**
     * 创建区域
     *
     * @param geoArea 区域描述
     */
    public static Geometry createGeometry(GeoArea geoArea) {
        if (geoArea.getType() == GeoArea.SHAPE_CIRCLE) {
            return createCircle(geoArea.getPoints().get(0));
        }
        if (geoArea.getType() == GeoArea.SHAPE_POLYGON) {
            return createPolygon(geoArea.getPoints(), false);
        }
        return null;
    }

    /**
     * 坐标位置
     *
     * @param point 坐标
     */
    public static Point createPoint(GeoPoint point) {
        Coordinate coordinate = new Coordinate(point.getLongitude(), point.getLatitude());
        Coordinate mercator = GeoConverts.geographic2Mercator(coordinate);
        return GEOMETRY_FACTORY.createPoint(new org.locationtech.jts.geom.Coordinate(mercator.x, mercator.y));
    }

    /**
     * 多边形区域
     *
     * @param points   坐标数据
     * @param original 是否原始数据，原始的话要处理下
     */
    public static Geometry createPolygon(List<GeoPoint> points, boolean original) {
        Assert.isTrue(CollectionUtils.isNotEmpty(points), "geoPointList can't be null");
        if (original) {
            // 原始单位换算
            for (GeoPoint geoPoint : points) {
                geoPoint.setLatitude(geoPoint.getLatitude() / 100000);
                geoPoint.setLongitude(geoPoint.getLongitude() / 100000);
            }
        }

        // 头尾相连
        points.add(points.get(0));

        //将经纬度坐标转换为墨卡托坐标
        List<Coordinate> mercatorList = new ArrayList<>();
        for (GeoPoint point : points) {
            Coordinate coordinate = new Coordinate(point.getLongitude(), point.getLatitude());
            Coordinate mercator = GeoConverts.geographic2Mercator(coordinate);
            mercatorList.add(mercator);
        }

        org.locationtech.jts.geom.Coordinate[] coordinates = new org.locationtech.jts.geom.Coordinate[mercatorList.size()];
        for (int i = 0; i < mercatorList.size(); i++) {
            Coordinate coordinate = mercatorList.get(i);
            coordinates[i] = new org.locationtech.jts.geom.Coordinate(coordinate.x, coordinate.y);
        }
        return GEOMETRY_FACTORY.createPolygon(coordinates);
    }

    /**
     * 圆形区域
     *
     * @param center 圆心
     */
    public static Geometry createCircle(GeoPoint center) {
        Assert.isTrue(!Objects.equals(center.getRadius(), 0.0), "圆形区域半径不能为0");
        Coordinate coordinate = new Coordinate(center.getLongitude(), center.getLatitude());
        Coordinate mercator = GeoConverts.geographic2Mercator(coordinate);
        //近似圆，圆心为center，半径为Radius，边数为NUMBER_EDGES_OF_POLYGON
        Point point = GEOMETRY_FACTORY.createPoint(new org.locationtech.jts.geom.Coordinate(mercator.x, mercator.y));
        return point.buffer(center.getRadius(), NUMBER_EDGES_OF_POLYGON);
    }

    /**
     * 区域是否包含位置
     *
     * @param area  区域
     * @param point 位置
     */
    public static boolean containsPoint(GeoArea area, GeoPoint point) {
        Geometry geometry = createGeometry(area);
        Point geopoint = createPoint(point);
        if(geometry != null){
            return geometry.contains(geopoint);
        }
        throw new IllegalArgumentException("区域构建失败");
    }

    /**
     * 区域是否包含区域
     *
     * @param src    包含区域
     * @param target 被包含区域
     */
    public static boolean containsArea(GeoArea src, GeoArea target) {
        Geometry geoSrc = createGeometry(src);
        Geometry geoTarget = createGeometry(target);
        if(geoSrc != null && geoTarget != null){
            return geoSrc.contains(geoTarget);
        }
        throw new IllegalArgumentException("区域构建失败");
    }
}
