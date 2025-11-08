package com.yzx.chatdemo;

import com.yzx.chatdemo.entity.Product;
import com.yzx.chatdemo.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ChatDemoApplicationTests {

    @Autowired
    private ProductService productService;
    @Test
    void contextLoads() {
        long count = productService.count();
        System.out.println("商品数量：" + count);
        for (Product product : productService.findAll()) {
            System.out.println(product);
        }
        boolean b = productService.existsById(1L);
        System.out.println(b);
    }

}
