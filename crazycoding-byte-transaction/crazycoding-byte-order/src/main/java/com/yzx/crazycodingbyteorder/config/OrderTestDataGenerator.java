// order/src/main/java/com/demo/order/OrderTestDataGenerator.java
package com.yzx.crazycodingbyteorder.config;

import com.yzx.crazycodingbyteorder.dto.CreateOrderRequest;
import com.yzx.crazycodingbyteorder.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderTestDataGenerator {
    
    private final OrderService orderService;
    
    @Bean
    public CommandLineRunner initTestData() {
        return args -> {
            log.info("开始生成测试数据...");
            // 生成测试订单
            CreateOrderRequest request = new CreateOrderRequest();
            request.setUserId(10001L);
            request.setProductId(1L);  // iPhone 15 Pro
            request.setProductName("iPhone 15 Pro");
            request.setProductPrice(new BigDecimal("8999.00"));
            request.setQuantity(1);
            request.setProductImage("https://example.com/iphone15pro.jpg");
            request.setProductSpec("256GB 深空黑色");
            // 注释掉，避免每次启动都创建订单
            // var result = orderService.createOrder(request);
            // log.info("测试订单创建结果：{}", result);
            
            log.info("测试数据生成完成");
        };
    }
}