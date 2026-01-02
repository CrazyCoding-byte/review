// pay/src/main/java/com/yzx/crazycodingbytepay/service/PayService.java
package com.yzx.crazycodingbytepay.service.impl;

import com.yzx.crazycodingbytepay.dto.PayRequest;
import com.yzx.crazycodingbytepay.dto.PayResponse;

public interface PayService {
    /**
     * 模拟支付处理
     */
    PayResponse processPayment(PayRequest request);
}