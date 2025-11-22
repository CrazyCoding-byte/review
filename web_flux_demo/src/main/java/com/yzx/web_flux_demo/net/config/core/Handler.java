package com.yzx.web_flux_demo.net.config.core;

/**
 * @className: Handler
 * @author: yzx
 * @date: 2025/11/23 0:32
 * @Version: 1.0
 * @description:
 */
@FunctionalInterface
public interface Handler {
    void handle(Context context) throws Exception;
}
