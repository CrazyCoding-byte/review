package com.yzx.crazycodingbytewms.listen;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @className: TestTagListen
 * @author: yzx
 * @date: 2026/1/8 5:49
 * @Version: 1.0
 * @description:
 */
@Component
@RocketMQMessageListener(consumerGroup = "testTagGroup",
        topic = "test_order_topic",
        selectorExpression = "tagA",
        selectorType = SelectorType.TAG,
        messageModel = MessageModel.CLUSTERING //集群模式是负载均衡 也就说如果集群模式下，多个消费者监听同一个主题，那么多个消费者会轮询接收消息
//        messageModel = MessageModel.BROADCASTING //广播模式，就是所有消费者都会接收到消息 比如说发送5个消息你有俩个listen 那么这俩个listen接受到5个消息总个就是10个
)
public class TestTagListen implements RocketMQListener<String> {


    @Override
    public void onMessage(String s) {
        System.out.println("接受到的tag消息" + s);
    }
}
