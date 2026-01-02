// pay/src/main/java/com/yzx/crazycodingbytepay/dto/PayRequest.java
package com.yzx.crazycodingbytepay.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayRequest {
    private String orderNo;          // 订单号
    private BigDecimal amount;       // 支付金额
    private String payType;          // 支付方式（ALIPAY/WXPAY）
    private String userId;           // 用户ID
}