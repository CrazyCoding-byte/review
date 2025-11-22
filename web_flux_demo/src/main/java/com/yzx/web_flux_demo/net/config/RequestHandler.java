package com.yzx.web_flux_demo.net.config;

/**
 * @className: Http2RequestHandler
 * @author: yzx
 * @date: 2025/11/22 17:02
 * @Version: 1.0
 * @description:
 */
@FunctionalInterface
public interface RequestHandler {
    void handle(Http2Request request, Http2ResponseCallback callback);
}
