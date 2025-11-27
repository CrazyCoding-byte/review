package com.yzx.web_flux_demo.flux;

import reactor.core.publisher.Mono;

/**
 * @className: FluxDemo
 * @author: yzx
 * @date: 2025/11/27 22:22
 * @Version: 1.0
 * @description:
 */
public class FluxDemo {
    public static void MonoDemo(){
        Mono<String> nihao = Mono.just("nihao");
        nihao.subscribe(System.out::println);
    }

    public static void main(String[] args) {
        MonoDemo();
    }
}
