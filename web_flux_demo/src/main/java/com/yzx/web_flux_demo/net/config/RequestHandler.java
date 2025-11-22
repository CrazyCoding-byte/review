package com.yzx.web_flux_demo.net.config;

import com.yzx.web_flux_demo.net.config.core.Request;

/**
 * @className: Http2RequestHandler
 * @author: yzx
 * @date: 2025/11/22 17:02
 * @Version: 1.0
 * @description:
 */
@FunctionalInterface
public interface RequestHandler {
    void handle(Request request, Http2ResponseCallback callback);
}
