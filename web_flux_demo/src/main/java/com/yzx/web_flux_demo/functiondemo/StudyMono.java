package com.yzx.web_flux_demo.functiondemo;

import reactor.core.publisher.Mono;

/**
 * @className: Mono
 * @author: yzx
 * @date: 2025/11/8 8:42
 * @Version: 1.0
 * @description:
 */
public class StudyMono {
    public static void main(String[] args) {
        // 1. 创建包含已知值的Mono，并订阅它（触发执行）
        Mono<String> mono = Mono.just("hello world");
        // 订阅：打印收到的值
        mono.subscribe(value -> System.out.println("Mono.just输出：" + value));

        // 2. 通过create手动发射数据，并订阅
        Mono.create(sink -> {
            sink.success("手动发射的数据"); // 发射值
        }).subscribe(
                value -> System.out.println("Mono.create输出：" + value), // 接收值
                error -> System.err.println("出错了：" + error), // 捕获错误
                () -> System.out.println("这个Mono的序列执行完啦～") // 完成回调
        );
    }
}
