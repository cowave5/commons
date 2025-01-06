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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import org.osgeo.proj4j.BasicCoordinateTransform;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.ProjCoordinate;

/**
 *
 * @author shanhuiming
 *
 */
class GeoConverts {

    /** 地球半径 */
    private static final double EARTH_RADIUS = 6378137.0;

    /** 地球周长 */
    private static final double EARTH_PERIMETER = 2 * Math.PI * EARTH_RADIUS;

    /** 瓦片大小,默认256 */
    private static final int TITLE_SIZE = 256;

    /** 初始像素分辨率 */
    private static final double INITIAL_RESOLUTION = EARTH_PERIMETER / TITLE_SIZE;

    /** 坐标原点 */
    private static final Coordinate ORIGIN = new Coordinate(-EARTH_PERIMETER / 2.0, EARTH_PERIMETER / 2.0);

    private static final BasicCoordinateTransform TRANSFORM_1;

    private static final BasicCoordinateTransform TRANSFORM_2;

    private static final CRSFactory CRS_FACTORY = new CRSFactory();

    private static final CoordinateReferenceSystem WGS_84_CRS = CRS_FACTORY.createFromName("EPSG:4326");

    private static final CoordinateReferenceSystem WEB_MERCATOR_CRS = CRS_FACTORY.createFromName("EPSG:3857");

    static {
        TRANSFORM_1 = new BasicCoordinateTransform(WGS_84_CRS, WEB_MERCATOR_CRS);
        TRANSFORM_2 = new BasicCoordinateTransform(WEB_MERCATOR_CRS, WGS_84_CRS);
    }

    /**
     * 缩放级别换算地图分辨率
     *
     * @param zoom 级别
     */
    private double zoomToResolution(int zoom) {
        return INITIAL_RESOLUTION / Math.pow(2, zoom);
    }

    /**
     * 经纬度转墨卡托
     *
     * @param pt 经纬度坐标
     * @return 墨卡托坐标
     */
    public static Coordinate geographic2Mercator(Coordinate pt) {
        synchronized (TRANSFORM_1) {
            ProjCoordinate pt1 = new ProjCoordinate(pt.x, pt.y);
            ProjCoordinate pt2 = new ProjCoordinate();
            TRANSFORM_1.transform(pt1, pt2);
            return new Coordinate(pt2.x, pt2.y);
        }
    }

    /**
     * 墨卡托转经纬度
     *
     * @param pt 墨卡托坐标
     * @return 经纬度坐标
     */
    public static Coordinate mercator2Geographic(Coordinate pt) {
        synchronized (TRANSFORM_2) {
            ProjCoordinate pt1 = new ProjCoordinate(pt.x, pt.y);
            ProjCoordinate pt2 = new ProjCoordinate();
            TRANSFORM_2.transform(pt1, pt2);
            return new Coordinate(pt2.x, pt2.y);
        }
    }

    /**
     * 高斯转经纬度
     *
     * @param pt 高斯坐标
     * @param d  度带号(3度带)
     * @return 经纬度坐标
     */
    public Coordinate gk2Geographic(Coordinate pt, int d) {
        synchronized (CRS_FACTORY) {
            CoordinateReferenceSystem gkcrs = CRS_FACTORY.createFromParameters("WGS84", String.format("+proj=tmerc +lat_0=0 +lon_0=%d +k=1 +x_0=500000 +y_0=0 +ellps=WGS84 +units=m +no_defs", d * 3));
            BasicCoordinateTransform transform = new BasicCoordinateTransform(gkcrs, WGS_84_CRS);
            ProjCoordinate pt1 = new ProjCoordinate(pt.x, pt.y);
            ProjCoordinate pt2 = new ProjCoordinate();
            transform.transform(pt1, pt2);
            return new Coordinate(pt2.x, pt2.y);
        }
    }

    /**
     * 经纬度转高斯
     *
     * @param pt 经纬度坐标
     * @return 高斯坐标
     */
    public static Coordinate geographic2Gk(Coordinate pt) {
        synchronized (CRS_FACTORY) {
            int d = (int) Math.floor((pt.y + 1.5) / 3);
            CoordinateReferenceSystem gkcrs = CRS_FACTORY.createFromParameters("WGS84", String.format("+proj=tmerc +lat_0=0 +lon_0=%d +k=1 +x_0=500000 +y_0=0 +ellps=WGS84 +units=m +no_defs", d * 3));
            BasicCoordinateTransform transform = new BasicCoordinateTransform(WGS_84_CRS, gkcrs);
            ProjCoordinate pt1 = new ProjCoordinate(pt.x, pt.y);
            ProjCoordinate pt2 = new ProjCoordinate();
            transform.transform(pt1, pt2);
            return new Coordinate(pt2.x, pt2.y);
        }
    }

    /**
     * 高斯转web墨卡托
     *
     * @param pt 高斯坐标
     * @param d  度带好(3度带)
     * @return 墨卡托坐标
     */
    public Coordinate gk2Mercator(Coordinate pt, int d) {
        synchronized (CRS_FACTORY) {
            CoordinateReferenceSystem gkcrs = CRS_FACTORY.createFromParameters("WGS84", String.format("+proj=tmerc +lat_0=0 +lon_0=%d +k=1 +x_0=500000 +y_0=0 +ellps=WGS84 +units=m +no_defs", d * 3));
            BasicCoordinateTransform transform = new BasicCoordinateTransform(gkcrs, WEB_MERCATOR_CRS);
            ProjCoordinate pt1 = new ProjCoordinate(pt.x, pt.y);
            ProjCoordinate pt2 = new ProjCoordinate();
            transform.transform(pt1, pt2);
            return new Coordinate(pt2.x, pt2.y);
        }
    }

    /**
     * 墨卡托转像素
     *
     * @param pt   墨卡托坐标
     * @param zoom 缩放级别
     * @return 像素坐标
     */
    public Pixel mercator2Pixel(Coordinate pt, int zoom) {
        double res = zoomToResolution(zoom);
        double px = (pt.x - ORIGIN.x) / res;
        double py = -(pt.y - ORIGIN.y) / res;

        //精度向下取整
        return new Pixel((long) Math.floor(px), (long) Math.floor(py));
    }

    /**
     * 像素转墨卡托
     *
     * @param pixel 像素坐标
     * @param zoom  缩放级别
     * @return 墨卡托坐标
     */
    public Coordinate pixel2Mercator(Pixel pixel, int zoom) {
        double res = zoomToResolution(zoom);
        double x = pixel.getX() * res + ORIGIN.x;
        double y = ORIGIN.y - pixel.getY() * res;
        return new Coordinate(x, y);
    }

    /**
     * 像素坐标所在瓦片
     *
     * @param pixel 像素坐标
     * @return 瓦片坐标
     */
    public Tile pixelAtTile(Pixel pixel) {
        long x = pixel.getX() / TITLE_SIZE;
        long y = pixel.getY() / TITLE_SIZE;
        return new Tile(x, y);
    }

    /**
     * 像素转瓦片内像素
     *
     * @param pixel 像素坐标
     * @param tile  瓦片坐标
     * @return 瓦片内像素坐标
     */
    private Pixel pixel2Tile(Pixel pixel, Tile tile) {
        long x = pixel.getX() - tile.getX() * TITLE_SIZE;
        long y = pixel.getY() - tile.getY() * TITLE_SIZE;
        return new Pixel(x, y);
    }

    /**
     * 瓦片内像素转像素
     *
     * @param p    瓦片内像素坐标
     * @param tile 瓦片坐标
     * @return 像素坐标
     */
    public Pixel tile2Pixel(Pixel p, Tile tile) {
        long x = p.getX() + tile.getX() * TITLE_SIZE;
        long y = p.getY() + tile.getY() * TITLE_SIZE;
        return new Pixel(x, y);
    }

    /**
     * 墨卡托转瓦片内像素
     *
     * @param pt   墨卡托坐标
     * @param tile 瓦片坐标
     * @param zoom 缩放级别
     * @return 瓦片内像素坐标
     */
    public Pixel mercator2Tile(Coordinate pt, Tile tile, int zoom) {
        Pixel p = mercator2Pixel(pt, zoom);
        return pixel2Tile(p, tile);
    }

    /**
     * 经纬度转像素
     *
     * @param pt   经纬度坐标
     * @param zoom 缩放级别
     * @return 像素坐标
     */
    public Pixel geographic2Pixel(Coordinate pt, int zoom) {
        Coordinate mpt = geographic2Mercator(pt);
        return mercator2Pixel(mpt, zoom);
    }

    /**
     * 经纬度转瓦片内像素
     *
     * @param pt   经纬度坐标
     * @param tile 瓦片坐标
     * @param zoom 缩放级别
     * @return 瓦片内像素坐标
     */
    public Pixel geographic2Tile(Coordinate pt, Tile tile, int zoom) {
        Pixel pixel = geographic2Pixel(pt, zoom);
        return this.pixel2Tile(pixel, tile);
    }

    /**
     * 像素转经纬度
     *
     * @param pixel 像素坐标
     * @param zoom  缩放级别
     * @return 经纬度坐标
     */
    public Coordinate pixel2Geographic(Pixel pixel, int zoom) {
        Coordinate mpt = pixel2Mercator(pixel, zoom);
        return mercator2Geographic(mpt);
    }

    /**
     * Tile坐标转换为所在的Tile的矩形
     *
     * @param tile 瓦片坐标
     * @return 矩形
     */
    public Envelope tile2Envelope(Tile tile) {
        long px = tile.getX() * TITLE_SIZE;
        long py = tile.getY() * TITLE_SIZE;
        Pixel pixel1 = new Pixel(px, py + 256);//左下
        Pixel pixel2 = new Pixel(px + 256, py);//右上
        Coordinate sw = pixel2Geographic(pixel1, tile.getZ());
        Coordinate ne = pixel2Geographic(pixel2, tile.getZ());
        return new Envelope(sw, ne);
    }

    /**
     * 瓦片坐标转换为QuadKey四叉树键值
     *
     * @param tile 瓦片坐标
     * @return String QuadKey四叉树键值
     */
    public String tile2QuadKey(Tile tile) {
        long x = tile.getX();
        long y = tile.getY();
        StringBuilder quadKey = new StringBuilder();
        for (int i = tile.getZ(); i > 0; i--) {
            char digit = '0';
            int mask = 1 << (i - 1);
            if ((x & mask) != 0) {
                digit++;
            }
            if ((y & mask) != 0) {
                digit++;
                digit++;
            }
            quadKey.append(digit);
        }
        return quadKey.toString();
    }

    /**
     * QuadKey四叉树键值转换为瓦片坐标
     *
     * @param quadKey QuadKey四叉树键值
     * @return 瓦片坐标
     */
    public Tile quadKey2Tile(String quadKey) {
        long x = 0;
        long y = 0;
        int levelOfDetail = quadKey.length();

        for (int i = levelOfDetail; i > 0; i--) {
            int mask = 1 << (i - 1);
            switch (quadKey.charAt(levelOfDetail - i)) {
                case '0':
                    break;
                case '1':
                    x |= mask;
                    break;
                case '2':
                    y |= mask;
                    break;
                case '3':
                    x |= mask;
                    y |= mask;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid QuadKey digit sequence.");
            }
        }
        return new Tile(x, y, levelOfDetail);
    }
}
