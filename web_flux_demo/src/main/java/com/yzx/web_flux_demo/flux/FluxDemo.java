package com.yzx.web_flux_demo.flux;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Arrays;
import java.util.Optional;

/**
 * @className: FluxDemo
 * @author: yzx
 * @date: 2025/11/27 22:22
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class FluxDemo {
    public static void MonoDemo() {
        Mono<String> nihao = Mono.just("nihao");
        Mono.empty();
        //从optional中获取值
        Mono<String> value = Mono.justOrEmpty(Optional.of("value"));
        //从callable中获取值
        Mono<String> stringMono = Mono.fromCallable(() -> "nihao");
        //延迟获取值
        Mono.defer(() -> Mono.just("nihao"));
//        Mono<String> error = Mono.error(new RuntimeException("error"));
        Mono<Integer> cast = Mono.just(111).cast(Integer.class);
        Mono<Tuple2<String, Integer>> zip = Mono.zip(nihao, Mono.just(123));
        zip.doOnNext(tuple -> System.out.println(tuple)).subscribe();
        cast.subscribe(System.out::println);
//        error.subscribe(System.out::println);
        nihao.subscribe(System.out::println);
    }

    public static void FluxDemo() {
        Flux<String> flux1 = Flux.just("A", "B", "C");
        Flux<Integer> range = Flux.range(1, 5);
        Flux<String> stringFlux = Flux.fromIterable(Arrays.asList("A", "B", "C"));
        Flux<Integer> map = flux1.map(String::length);
        flux1.subscribe(System.out::println);
        range.subscribe(System.out::println);
    }

    public static void main(String[] args) {
        FluxDemo();
    }
}
