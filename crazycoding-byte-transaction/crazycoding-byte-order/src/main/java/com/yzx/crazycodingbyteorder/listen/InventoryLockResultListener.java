// order/src/main/java/com/demo/order/mq/InventoryLockResultListener.java
package com.yzx.crazycodingbyteorder.listen;

import com.yzx.crazycodingbyteorder.dto.InventoryLockResultDTO;
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
@RocketMQMessageListener(topic = "INVENTORY_LOCK_RESULT_TOPIC", consumerGroup = "order-inventory-lock-result-consumer-group")
public class InventoryLockResultListener implements RocketMQListener<org.apache.rocketmq.common.message.MessageExt> {

    private final OrderService orderService;

    /**
     * 接受消息的格式
     * 消息头:
     * 发送端:MessageBuilder.setHeader("key",value)
     * 接收端:message.getHeaders().get("key")
     * 用户属性:
     * 	发送端：MessageBuilder.setHeader(RocketMQHeaders.USER_PROPERTIES, map) 或 MessageExt.putUserProperty
     * 接收端：messageExt.getUserProperty("key")
     * 消息体（Payload）:
     * 发送端：MessageBuilder.withPayload(对象)（默认 JSON / 字节数组）
     * 接收端：new String(messageExt.getBody()) 后反序列化为对象
     * @param message
     */
    @Override
    public void onMessage(org.apache.rocketmq.common.message.MessageExt message) {
        String msgId = message.getMsgId();
        try {
            // 1. 解析消息体（核心：把二进制body反序列化为DTO）

            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            InventoryLockResultDTO resultDTO = com.alibaba.fastjson2.JSON.parseObject(body, InventoryLockResultDTO.class);

            // 2. 从DTO中取字段（替代UserProperty）
            String orderNo = resultDTO.getOrderNo();
            boolean lockSuccess = resultDTO.isLockSuccess();
            String errorMsg = resultDTO.getFailReason();

            log.info("收到库存锁定结果消息，msgId：{}，订单号：{}，锁定结果：{}，错误信息：{}",
                    msgId, orderNo, lockSuccess, errorMsg);

            // 3. 调用处理方法
            orderService.handleInventoryLockResult(orderNo, lockSuccess, errorMsg);
        } catch (Exception e) {
            log.error("处理库存锁定结果消息失败，msgId：{}", msgId, e);
            throw new RuntimeException(e);
        }
    }
}