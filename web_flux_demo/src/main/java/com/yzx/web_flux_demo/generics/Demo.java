package com.yzx.web_flux_demo.generics;

import java.util.function.Consumer;
import java.util.function.Function;

public class Demo {
    public static void main(String[] args) {
//        int calculate = calculate(new IntBinaryOperator() {
//            @Override
//            public int applyAsInt(int left, int right) {
//                return left + right;
//            }
//        });
//        System.out.println(calculate);
//        test(new IntPredicate() {
//            @Override
//            public boolean test(int i) {
//                return i % 2 == 0;
//            }
//        });

        //main-> 接受函数式接口逻辑 遍历 使用函数式接口逻辑 ->返回值
        test(i -> i % 2 == 0);
        Integer o = tyConvert(new Function<String, Integer>() {
            @Override
            public Integer apply(String s) {
                return Integer.valueOf(s);
            }
        }, "1234");
        System.out.println(o);
    }

    public static int calculate(IntBinaryOperator operator) {
        int a = 10;
        int b = 20;
        return operator.applyAsInt(a, b);
    }

    //这个<> 表示返回的类型是未知 如果不带 R就是一个类
    public static <R> R tyConvert(Function<String, R> function, String message) {
        return function.apply(message);
    }


    public static void test(IntPredicate p) {
        int[] arr = new int[]{1, 2, 3, 4, 5};
        for (int i : arr) {
            if (p.test(i)) {
                System.out.println(i);
            }
        }
    }

    public static <T> void consumer(Consumer<T> consumer, T t) {
        consumer.accept(t);
    }
}
