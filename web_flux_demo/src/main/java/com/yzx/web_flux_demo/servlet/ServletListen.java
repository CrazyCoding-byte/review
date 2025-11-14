package com.yzx.web_flux_demo.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * @className: ServletListen
 * @author: yzx
 * @date: 2025/9/10 17:29
 * @Version: 1.0
 * @description:
 */
@WebListener
public class ServletListen implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("WebApp initialized.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("WebApp destroyed.");
    }
}
