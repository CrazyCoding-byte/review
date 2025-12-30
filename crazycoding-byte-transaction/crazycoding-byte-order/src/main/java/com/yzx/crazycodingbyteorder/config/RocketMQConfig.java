// order/src/main/java/com/demo/order/config/RocketMQConfig.java
package com.yzx.crazycodingbyteorder.config;

import com.yzx.crazycodingbyteorder.listen.OrderTransactionListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.spring.annotation.ExtRocketMQTemplateConfiguration;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ExtRocketMQTemplateConfiguration(nameServer = "${spring.rocketmq.name-server}")
public class RocketMQConfig {

    @Bean
    public RocketMQTemplate rocketMQTemplate(@Value("${spring.rocketmq.name-server}") String nameServer) {
        RocketMQTemplate template = new RocketMQTemplate();

        // 配置生产者
        DefaultMQProducer producer = new DefaultMQProducer("order-service-producer-group");
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(3000);
        producer.setRetryTimesWhenSendFailed(2);

        // 配置事务生产者
        TransactionMQProducer transactionProducer = new TransactionMQProducer("order-service-transaction-producer-group");
        transactionProducer.setNamesrvAddr(nameServer);
        transactionProducer.setSendMsgTimeout(3000);
        transactionProducer.setRetryTimesWhenSendFailed(2);

        // 设置事务监听器
        transactionProducer.setTransactionListener(orderTransactionListener());

        template.setProducer(transactionProducer);

        return template;
    }

    @Bean
    public OrderTransactionListener orderTransactionListener() {
        return new OrderTransactionListener();
    }
}