package com.yzx.web_flux_demo.net.config;

import com.yzx.web_flux_demo.net.handler.Http2RequestHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * @className: Router
 * @author: yzx
 * @date: 2025/11/22 16:40
 * @Version: 1.0
 * @description:
 */
@Slf4j
@Data
public class Route {
    private final String method;
    private final String path;
    private final Pattern pathPattern;
    private final RequestHandler handler; // 使用重构后的 RequestHandler 接口

    public Route(String method, String path, Pattern pathPattern, RequestHandler handler) {
        this.method = method.toUpperCase();
        this.path = path;
        this.pathPattern = pathPattern;
        this.handler = handler;
    }
}
