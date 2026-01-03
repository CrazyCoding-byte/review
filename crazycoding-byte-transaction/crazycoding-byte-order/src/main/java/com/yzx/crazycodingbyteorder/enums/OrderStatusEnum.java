// order/src/main/java/com/demo/order/enums/OrderStatusEnum.java
package com.yzx.crazycodingbyteorder.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    WAIT_PAY(0, "待支付"),
    PAID(1, "已支付"),
    CANCELED(2, "已取消"),
    COMPLETED(3, "已完成"),
    LOCKING(4, "锁定中"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款"),
    CLOSED(7, "已关闭"),
    REFUND_FAILED(8, "退款失败"),
    CANCEL_FAILED(9, "取消失败"),
    PAY_FAILED(10, "支付失败"),
    LOCK_FAIL(11, "锁定失败");

    private final Integer code;
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatusEnum getByCode(Integer code) {
        for (OrderStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}