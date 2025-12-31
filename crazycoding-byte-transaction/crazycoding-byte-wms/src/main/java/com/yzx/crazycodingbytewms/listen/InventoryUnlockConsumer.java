// wms/src/main/java/com/yzx/crazycodingbytemms/consumer/InventoryUnlockConsumer.java
package com.yzx.crazycodingbytewms.listen;

import com.yzx.crazycodingbytewms.constant.OrderConstant;
import com.yzx.crazycodingbytewms.dto.InventoryLockDTO;
import com.yzx.crazycodingbytewms.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 接收订单服务的解锁库存请求（MQ消费者）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = OrderConstant.TOPIC_INVENTORY_UNLOCK,
        consumerGroup = "inventory_unlock_consumer_group"
)
public class InventoryUnlockConsumer implements RocketMQListener<InventoryLockDTO> {

    private final InventoryService inventoryService;

    @Override
    public void onMessage(InventoryLockDTO unlockDTO) {
        try {
            boolean unlockSuccess = inventoryService.unlockStock(unlockDTO);
            if (unlockSuccess) {
                log.info("解锁库存成功，订单号：{}", unlockDTO.getOrderNo());
            } else {
                log.error("解锁库存失败，订单号：{}", unlockDTO.getOrderNo());
                // 失败可抛异常，让MQ重试（或进死信队列）
                throw new RuntimeException("解锁库存失败，订单号：" + unlockDTO.getOrderNo());
            }
        } catch (Exception e) {
            log.error("处理解锁库存请求异常，订单号：{}", unlockDTO.getOrderNo(), e);
            throw e; // 触发MQ重试
        }
    }
}