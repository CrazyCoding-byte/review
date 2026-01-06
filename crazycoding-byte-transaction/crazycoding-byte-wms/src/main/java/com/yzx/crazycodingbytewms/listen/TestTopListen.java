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
        topic = "delay_topic", // 监听的业务主题
        consumerGroup = "test-consumer-group"// 与yml中一致
)
public class TestTopListen implements RocketMQListener<String> {

    @Override
    public void onMessage(String s) {
        System.out.println("接受时间"+System.currentTimeMillis());
        System.out.println("接收到延迟消息：" + s);
    }
}
