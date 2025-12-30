// order/src/main/java/com/demo/order/enums/OrderStatusEnum.java
package com.yzx.crazycodingbyteorder.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    WAIT_PAY(0, "待支付"),
    PAID(1, "已支付"),
    CANCELED(2, "已取消"),
    COMPLETED(3, "已完成"),
    ;
    
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