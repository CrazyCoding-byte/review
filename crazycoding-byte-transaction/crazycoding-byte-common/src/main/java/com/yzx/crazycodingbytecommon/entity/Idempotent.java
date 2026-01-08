package com.yzx.crazycodingbytecommon.entity;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 通用幂等注解
 * 作用：标注在需要幂等的方法上，底层AOP自动实现幂等校验
 */
@Target({ElementType.METHOD}) // 仅作用于方法
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
@Documented
public @interface Idempotent {
    /**
     * 幂等键的SpEL表达式（核心：动态获取订单号/请求ID等唯一标识）
     * 示例：'LOCK_STOCK_'+#lockDTO.orderNo  → 最终生成 KEY: LOCK_STOCK_20260108123456
     * 示例：'CREATE_ORDER_'+#request.requestId → 最终生成 KEY: CREATE_ORDER_123456789
     */
    String key() default "";

    /**
     * 幂等键过期时间（默认1天，防止Redis键堆积）
     */
    long expireTime() default 86400;

    /**
     * 过期时间单位（默认秒）
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 重复操作时的提示语
     */
    String message() default "请勿重复操作";
}