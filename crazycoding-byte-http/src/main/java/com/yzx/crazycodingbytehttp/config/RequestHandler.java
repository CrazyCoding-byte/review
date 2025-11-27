package com.yzx.crazycodingbytehttp.config;


import com.yzx.crazycodingbytehttp.config.core.Request;

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
