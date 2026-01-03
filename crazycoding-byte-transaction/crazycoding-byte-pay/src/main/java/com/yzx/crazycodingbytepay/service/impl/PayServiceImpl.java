// pay/src/main/java/com/yzx/crazycodingbytepay/service/impl/PayServiceImpl.java
package com.yzx.crazycodingbytepay.service.impl;


import com.yzx.crazycodingbytepay.dto.PayRequest;
import com.yzx.crazycodingbytepay.dto.PayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final LocalMessageService localMessageService;

    /**
     * 模拟支付流程：
     * 1. 生成支付单号
     * 2. 模拟支付处理（成功/失败）
     * 3. 支付结果同步更新本地消息表状态
     */
    @Transactional
    @Override
    public PayResponse processPayment(PayRequest request) {
        // 1. 生成支付单号
        String payNo = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("开始处理支付，订单号：{}，支付单号：{}", request.getOrderNo(), payNo);

        // 2. 模拟支付处理（这里用随机数模拟成功/失败，实际应调用第三方支付接口）
        boolean paySuccess = simulatePaymentProcessing(request);

        // 3. 支付结果与本地消息表联动 支付成功之后发送消息给本地消息表 然后轮询本地消息表去实际的扣减库存和解锁库存
        if (paySuccess) {
            // 支付成功：更新本地消息为"可发送"状态（或直接触发发送）
            //修改订单状态
            //发送本地消息
            localMessageService.updateMessageStatusForPaymentSuccess(request.getOrderNo());
            log.info("支付成功，订单号：{}，支付单号：{}", request.getOrderNo(), payNo);
            return PayResponse.success(payNo, request.getOrderNo(), "支付成功");
        } else {
            // 支付失败：更新本地消息为"失败"状态
            localMessageService.updateMessageStatusForPaymentFailed(request.getOrderNo(), "模拟支付失败");
            log.warn("支付失败，订单号：{}，支付单号：{}", request.getOrderNo(), payNo);
            return PayResponse.failure(payNo, request.getOrderNo(), "支付失败");
        }
    }

    /**
     * 模拟支付处理逻辑
     */
    private boolean simulatePaymentProcessing(PayRequest request) {
        try {
            // 模拟网络延迟
            Thread.sleep(1000);
            // 80%成功率
            return Math.random() < 0.8;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}