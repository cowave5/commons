/*
 * Copyright (c) 2017～2024 Cowave All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.commons.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.cowave.commons.response.HttpResponseCode.INTERNAL_SERVER_ERROR;
import static com.cowave.commons.response.HttpResponseCode.SUCCESS;

/**
 * @author shanhuiming
 */
@Slf4j
@Data
public class Response<T> {

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应码
     */
    private String code;

    /**
     * 响应描述
     */
    private String msg;

    /**
     * 错误堆栈信息
     */
    private List<String> cause;

    public Response() {

    }

    public Response(String code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    @Override
    public String toString() {
        return "{code=" + code + ", msg=" + msg + ", data=" + data + "}";
    }

    /**
     * status=200, code=#{responseCode.code}, msg=#{responseCode.msg}, data=null
     */
    public static <V> Response<V> code(ResponseCode responseCode) {
        return new Response<>(responseCode.getCode(), responseCode.getMsg(), null);
    }

    /**
     * status=200, code=#{responseCode.code}, msg=#{responseCode.msg}, data=#{data}
     */
    public static <V> Response<V> data(ResponseCode responseCode, V data) {
        return new Response<>(responseCode.getCode(), responseCode.getMsg(), data);
    }

    /**
     * status=200, code=#{resp.code}, msg=#{msg}, data=null
     */
    public static <V> Response<V> msg(ResponseCode responseCode, String msg) {
        return new Response<>(responseCode.getCode(), msg, null);
    }

    /**
     * status=200, code=200, msg="success", data=null
     */
    public static <V> Response<V> success() {
        return new Response<>(SUCCESS.getCode(), SUCCESS.getMsg(), null);
    }

    /**
     * status=200, code=200, msg="success", data=#{data}
     */
    public static <V> Response<V> success(V data) {
        return new Response<>(SUCCESS.getCode(), SUCCESS.getMsg(), data);
    }

    /**
     * status=200, code=200, msg=#{msg}, data=#{data}
     */
    public static <V> Response<V> success(V data, String msg) {
        return new Response<>(SUCCESS.getCode(), msg, data);
    }

    /**
     * status=200, code=500, msg="Internal Server Error", data=null
     */
    public static <V> Response<V> error() {
        return new Response<>(INTERNAL_SERVER_ERROR.getCode(), INTERNAL_SERVER_ERROR.getMsg(), null);
    }

    /**
     * status=200, code=500, msg=#{msg}, data=null
     */
    public static <V> Response<V> error(String msg) {
        return new Response<>(INTERNAL_SERVER_ERROR.getCode(), msg, null);
    }

    /**
     * status=200, code=200, msg="success", data=#{page}
     */
    public static <E> Response<Page<E>> page(List<E> list) {
        Response<Page<E>> response = new Response<>(SUCCESS.getCode(), SUCCESS.getMsg(), null);
        if (list == null) {
            list = new ArrayList<>();
        }

        if (list instanceof com.github.pagehelper.Page<E> page) {
            response.setData(new Page<>(page, page.getTotal()));
        } else {
            response.setData(new Page<>(list, list.size()));
        }
        return response;
    }

    /**
     * status=200, code=200, msg="success", data=#{page}
     */
    public static <E> Response<Page<E>> page(com.baomidou.mybatisplus.extension.plugins.pagination.Page<E> page) {
        Response<Page<E>> response = new Response<>(SUCCESS.getCode(), SUCCESS.getMsg(), null);
        response.setData(new Page<>(page.getRecords(), page.getTotal()));
        return response;
    }

    /**
     * status=200, code=200, msg="success", data=#{page}
     */
    public static <T, E> Response<Page<E>> page(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page, Class<E> clazz) {
        Response<Page<E>> response = new Response<>(SUCCESS.getCode(), SUCCESS.getMsg(), null);
        response.setData(new Page<>(copyList(page.getRecords(), clazz), page.getTotal()));
        return response;
    }

    /**
     * status=200, code=200, msg="success", data=#{page}
     */
    public static <T, E> Response<Page<E>> page(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page, Function<T, E> mapper) {
        Response<Page<E>> response = new Response<>(SUCCESS.getCode(), SUCCESS.getMsg(), null);
        response.setData(new Page<>(page.getRecords().stream().map(mapper).toList(), page.getTotal()));
        return response;
    }

    /**
     * status=200, code=200, msg="success", data=#{page}
     */
    public static <T, E> Response<Page<E>> page(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page, Function<T, E> mapper, Predicate<T> filter) {
        Response<Page<E>> response = new Response<>(SUCCESS.getCode(), SUCCESS.getMsg(), null);
        response.setData(new Page<>(page.getRecords().stream().filter(filter).map(mapper).toList(), page.getTotal()));
        return response;
    }

    static <E, T> E copyBean(T src, Class<E> clazz) {
        E target = null;
        try {
            target = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        BeanUtils.copyProperties(src, target);
        return target;
    }

    static <E, T> List<E> copyList(List<T> srcList, Class<E> clazz) {
        if (CollectionUtils.isEmpty(srcList)) {
            return Collections.emptyList();
        }
        return srcList.stream().map(src -> copyBean(src, clazz)).toList();
    }

    @Data
    public static class Page<E> {

        /**
         * 总数
         */
        private int total;

        /**
         * 列表数据
         */
        private Collection<E> list;

        /**
         * 页码
         */
        @JsonIgnore
        private int page;

        /**
         * 每页行数
         */
        @JsonIgnore
        private int pageSize;

        /**
         * 总页数
         */
        @JsonIgnore
        private int totalPage;

        public Page() {

        }

        public Page(Collection<E> list, int total) {
            this.list = list;
            this.total = total;
        }

        public Page(Collection<E> list, long total) {
            this.list = list;
            this.total = (int) total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public void setTotal(long total) {
            this.total = (int) total;
        }

        public void setTotalPage(int totalPage) {
            this.totalPage = totalPage;
        }

        public void setTotalPage(long totalPage) {
            this.totalPage = (int) totalPage;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public void setPage(long page) {
            this.page = (int) page;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public void setPageSize(long pageSize) {
            this.pageSize = (int) pageSize;
        }

        @Override
        public String toString() {
            return "{total=" + total + ", list=" + list + "}";
        }
    }
}
