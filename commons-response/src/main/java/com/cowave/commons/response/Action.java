package com.cowave.commons.response;

/**
 * @author shanhuiming
 */
@FunctionalInterface
public interface Action {

    void exec() throws Exception;
}
