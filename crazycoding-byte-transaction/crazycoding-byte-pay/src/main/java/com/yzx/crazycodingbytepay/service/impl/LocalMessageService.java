// order/src/main/java/com/yzx/crazycodingbyteorder/service/LocalMessageService.java
package com.yzx.crazycodingbytepay.service.impl;

public interface LocalMessageService {
    /**
     * 支付成功时更新消息状态
     */
    void updateMessageStatusForPaymentSuccess(String orderNo);

    /**
     * 支付失败时更新消息状态
     */
    void updateMessageStatusForPaymentFailed(String orderNo, String errorMsg);
}