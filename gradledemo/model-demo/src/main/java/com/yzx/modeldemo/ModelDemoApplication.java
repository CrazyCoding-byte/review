package com.yzx.modeldemo;

import com.yzx.chatdemo.entity.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ModelDemoApplication {

    public static void main(String[] args) {
        new User();
        SpringApplication.run(ModelDemoApplication.class, args);
    }

}
