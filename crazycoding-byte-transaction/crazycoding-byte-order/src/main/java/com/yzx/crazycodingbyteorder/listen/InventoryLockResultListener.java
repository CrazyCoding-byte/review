// order/src/main/java/com/demo/order/mq/InventoryLockResultListener.java
package com.yzx.crazycodingbyteorder.listen;

import com.yzx.crazycodingbyteorder.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "INVENTORY_LOCK_RESULT_TOPIC",
        consumerGroup = "order-inventory-lock-result-consumer-group"
)
public class InventoryLockResultListener implements RocketMQListener<org.apache.rocketmq.common.message.MessageExt> {
    
    private final OrderService orderService;
    
    @Override
    public void onMessage(org.apache.rocketmq.common.message.MessageExt message) {
        String msgId = message.getMsgId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String orderNo = message.getUserProperty("ORDER_NO");
        String lockResult = message.getUserProperty("LOCK_RESULT");
        String errorMsg = message.getUserProperty("ERROR_MSG");
        
        log.info("收到库存锁定结果消息，msgId：{}，订单号：{}，锁定结果：{}，错误信息：{}",
                msgId, orderNo, lockResult, errorMsg);
        
        try {
            boolean lockSuccess = "SUCCESS".equals(lockResult);
            orderService.handleInventoryLockResult(orderNo, lockSuccess, errorMsg);
        } catch (Exception e) {
            log.error("处理库存锁定结果消息失败，msgId：{}", msgId, e);
            // 抛出异常，让RocketMQ重试
            throw new RuntimeException(e);
        }
    }
}