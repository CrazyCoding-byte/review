// wms/src/main/java/com/yzx/crazycodingbytemms/consumer/InventoryLockConsumer.java
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
 * MQ消费者：接收订单服务的锁库存请求
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = OrderConstant.TOPIC_INVENTORY_LOCK, // 和订单服务的主题一致
        consumerGroup = "inventory_lock_consumer_group" // 消费者组唯一
)
public class InventoryLockConsumer implements RocketMQListener<InventoryLockDTO> {

    private final InventoryService inventoryService;

    @Override
    public void onMessage(InventoryLockDTO lockDTO) {
        try {
            // 调用锁库存核心方法
            boolean lockSuccess = inventoryService.lockStock(lockDTO);
            
            // 5. 给订单服务回传锁库存结果（可选，用MQ回调）
            if (lockSuccess) {
                log.info("锁库存成功，通知订单服务创建订单，订单号：{}", lockDTO.getOrderNo());
                // TODO: 发MQ消息给订单服务（TOPIC_INVENTORY_LOCK_RESULT），告知锁成功
            } else {
                log.error("锁库存失败，通知订单服务，订单号：{}", lockDTO.getOrderNo());
                // TODO: 发MQ消息给订单服务，告知锁失败
            }
        } catch (Exception e) {
            log.error("处理锁库存请求异常，订单号：{}", lockDTO.getOrderNo(), e);
        }
    }
}