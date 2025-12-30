package com.yzx.crazycodingbytecommon.entity;

import lombok.Getter;

/**
 * @className: ResultCode
 * @author: yzx
 * @date: 2025/12/30 20:59
 * @Version: 1.0
 * @description:
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数检验失败"),
    UNAUTHORIZED(401, "暂未登录或token已经过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在"),

    // 业务错误码
    ORDER_NOT_FOUND(10001, "订单不存在"),
    INVENTORY_NOT_ENOUGH(10002, "库存不足"),
    PAYMENT_FAILED(10003, "支付失败"),

    // 系统错误码
    SYSTEM_ERROR(50001, "系统内部错误"),
    SERVICE_UNAVAILABLE(50002, "服务暂不可用");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
