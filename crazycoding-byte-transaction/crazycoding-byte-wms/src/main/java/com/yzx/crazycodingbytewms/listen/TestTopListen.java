package com.yzx.crazycodingbytewms.listen;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @className: TestTopListen
 * @author: yzx
 * @date: 2026/1/4 13:04
 * @Version: 1.0
 * @description:
 */
@Component
// 5.x 消费者注解（核心：maxReconsumeTimes=0 触发死信）
@RocketMQMessageListener(
        topic = "test_top", // 监听的业务主题
        consumerGroup = "test-consumer-group", // 与yml中一致
        consumeMode = ConsumeMode.CONCURRENTLY, // 并发消费
        messageModel = MessageModel.CLUSTERING, // 集群模式（默认）
        maxReconsumeTimes = 0 // 优先级最高：重试次数为0，失败/超时直接进死信
)
public class TestTopListen implements RocketMQListener<String> {

    @Override
    public void onMessage(String s) {
        // 模拟「未消费/消费失败」场景：
        // 场景1：故意抛异常 → 消费失败，无重试直接进死信
        // 场景2：不处理 → 消息超时未ACK，触发死信
        System.out.println("接收到消息：" + s);
        throw new RuntimeException("模拟消费失败，触发死信队列");
    }
}
