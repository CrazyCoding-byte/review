package com.yzx.web_flux_demo.generics;

/**
 * @className: IntPredicate
 * @author: yzx
 * @date: 2025/9/9 20:09
 * @Version: 1.0
 * @description:
 */
@FunctionalInterface
public interface IntPredicate {
    public boolean test(int i);
}
