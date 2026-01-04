//package com.yzx.crazycodingbytepay.listen;
//
//import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
//import org.apache.rocketmq.spring.core.RocketMQListener;
//import org.springframework.stereotype.Component;
//
///**
// * @className: TestTopConsumer
// * @author: yzx
// * @date: 2026/1/4 12:17
// * @Version: 1.0
// * @description:
// */
//// 测试2：消费者（监听 test_top 主题的消息）
//@Component  // 必须加@Component，让Spring扫描到
//@RocketMQMessageListener(
//        topic = "test_top",        // 监听的主题名（和你创建的一致）
//        consumerGroup = "test_consumer_group"  // 消费者组名（自定义）
//)
//public class TestTopConsumer implements RocketMQListener<String> {
//    // 接收到消息时触发这个方法
//    @Override
//    public void onMessage(String message) {
//        System.out.println("📩 接收到 test_top 主题的消息：" + message);
//    }
//}