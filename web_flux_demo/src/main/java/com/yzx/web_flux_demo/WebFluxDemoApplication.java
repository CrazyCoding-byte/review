package com.yzx.web_flux_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class WebFluxDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebFluxDemoApplication.class, args);
    }

}
