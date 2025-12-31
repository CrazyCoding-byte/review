package com.yzx.crazycodingbyteorder.config;

import com.yzx.crazycodingbyteorder.listen.OrderTransactionListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.GsonMessageConverter;

// 注意：移除@ExtRocketMQTemplateConfiguration（该注解是扩展模板用的，不是全局配置）
@Slf4j
@Configuration
public class RocketMQConfig {


    // ========== 3. RocketMQTemplate（绑定普通生产者） ==========
    @Bean
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer producer) {
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(producer);
        // 替换为自定义的消息转换器
        template.setMessageConverter(new GsonMessageConverter());
        return template;
    }


}