package com.yzx.crazycodingbytegateway.config;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.http.HttpClient;

/**
 * @className: RouteForward
 * @author: yzx
 * @date: 2025/11/14 1:23
 * @Version: 1.0
 * @description:
 */
public class RouteForward {
    private final HttpClient asyncHttpClient;

    public void forwardRequest(FullHttpRequest requestl, RouteConfig config, ChannelHandlerContext context) {
        try {

        }
    }

    private String buildTargetUrl(FullHttpRequest request, RouteConfig config) {
        String uri = request.getUri();
        if (uri.startsWith(config.getPath())) {
            String substring = uri.substring(config.getPath().length());
            return substring;
        }
        return config.getTargetUrl() + uri;
    }
}
