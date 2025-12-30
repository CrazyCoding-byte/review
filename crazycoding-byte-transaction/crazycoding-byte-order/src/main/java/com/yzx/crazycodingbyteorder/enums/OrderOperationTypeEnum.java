// order/src/main/java/com/demo/order/enums/OrderOperationTypeEnum.java
package com.yzx.crazycodingbyteorder.enums;

import lombok.Getter;

@Getter
public enum OrderOperationTypeEnum {
    CREATE("CREATE", "创建订单"),
    PAY("PAY", "订单支付"),
    CANCEL("CANCEL", "取消订单"),
    UPDATE("UPDATE", "更新订单"),
    ;
    
    private final String code;
    private final String desc;
    
    OrderOperationTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}