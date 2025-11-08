package com.yzx.web_flux_demo.functiondemo;

/**
 * @className: Mono
 * @author: yzx
 * @date: 2025/11/8 8:42
 * @Version: 1.0
 * @description:
 */
public class StudyMono {
    public static void main(String[] args) {
        Mono<String> mono = Mono.just("hello world");
    }
}
