package com.cowave.commons.framework.filter.repeat;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author shanhuiming
 *
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatLimit{

    /**
     * 间隔时间(ms)
     */
    int interval() default 5000;

    /**
     * 提示消息
     */
    String message() default "repeated request";
}
