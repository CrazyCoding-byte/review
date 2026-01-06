package com.yzx.crazycodingbytewms.listen;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @className: TestOrderListen
 * @author: yzx
 * @date: 2026/1/5 8:18
 * @Version: 1.0
 * @description:
 */
@Component
@RocketMQMessageListener(consumerGroup = "test_order_consumer",
        topic = "test_order", consumeThreadNumber = 5,consumeMode = ConsumeMode.ORDERLY)
public class TestOrderListen implements RocketMQListener<String> {

    @Override
    public void onMessage(String s) {
        try {
            // 随机休眠0-500ms，模拟业务处理耗时，让多线程有机会抢消息
            Thread.sleep(new Random().nextInt(500));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.printf("线程[%s]接受到的消息:%s\n", Thread.currentThread().getName(), s);
    }
}
