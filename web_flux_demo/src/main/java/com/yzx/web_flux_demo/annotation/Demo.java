package com.yzx.web_flux_demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @className: Demo
 * @author: yzx
 * @date: 2025/11/13 18:04
 * @Version: 1.0
 * @description:
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Demo {
    String value() default "yzx";

    int age() default 18;
}
