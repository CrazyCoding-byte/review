package com.yzx.web_flux_demo.generics;

/**
 * @className: IntBinaryOperator
 * @author: yzx
 * @date: 2025/9/9 18:48
 * @Version: 1.0
 * @description:
 */
@FunctionalInterface
public interface IntBinaryOperator {
    public int applyAsInt(int left, int right);
}
