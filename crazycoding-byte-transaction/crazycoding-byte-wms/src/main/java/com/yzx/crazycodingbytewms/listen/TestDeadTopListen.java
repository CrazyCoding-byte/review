package com.yzx.crazycodingbytewms.listen;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @className: TestDeadTopListen
 * @author: yzx
 * @date: 2026/1/4 14:15
 * @Version: 1.0
 * @description:
 */
@Component
// 监听5.x死信队列：DLQ_ + 原业务消费者组名
@RocketMQMessageListener(
        topic = "DLQ_test-consumer-group", // 5.x 死信队列主题
        consumerGroup = "dlq-consumer-group", // 死信消费者组（需与业务组不同）
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class TestDeadTopListen implements RocketMQListener<String> {
    @Override
    public void onMessage(String message) {
        // 处理死信消息的核心逻辑：
        // 1. 记录日志（便于排查未消费原因）
        // 2. 人工介入/告警
        // 3. 可选：重新发送到业务主题（需防循环死信）
        System.out.println("【死信队列】处理过期/失败消息：" + message);

        // 示例：重新发送到业务主题（可选，需加防重逻辑）
        // rocketMQTemplate.syncSend("test-topic", message);
    }
}
