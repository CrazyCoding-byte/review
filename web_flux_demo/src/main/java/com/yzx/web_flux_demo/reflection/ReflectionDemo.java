package com.yzx.web_flux_demo.reflection;

import com.yzx.web_flux_demo.annotation.Demo;

/**
 * @className: ReflectionDemo
 * @author: yzx
 * @date: 2025/11/12 16:51
 * @Version: 1.0
 * @description:
 */
public class ReflectionDemo {
    public static void main(String[] args) {
        try {
            Class<?> clazz = Class.forName("com.yzx.web_flux_demo.entity.Book");
            Object obj = clazz.newInstance();
            Demo annotation = clazz.getAnnotation(Demo.class);
            System.out.println(annotation.age());
            System.out.println(obj.getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
