package com.yzx.crazycodingbytewms.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.GsonMessageConverter;

/**
 * @className: RocketMQConfig
 * @author: yzx
 * @date: 2026/1/3 11:55
 * @Version: 1.0
 * @description:
 */
@Configuration
public class RocketMQConfig {

    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(new DefaultMQProducer());
        // 替换为自定义的消息转换器
        template.setMessageConverter(new GsonMessageConverter());
        return template;
    }
}
