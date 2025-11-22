package com.yzx.web_flux_demo.net.config;

import com.yzx.web_flux_demo.net.config.core.Handler;
import com.yzx.web_flux_demo.net.handler.Http2RequestHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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
    private final List<Handler> middlewares; // 新增：存储中间件列表

    // 修改构造函数
    public Route(String method, String path, Pattern pathPattern, RequestHandler handler, List<Handler> middlewares) {
        this.method = method.toUpperCase();
        this.path = path;
        this.pathPattern = pathPattern;
        this.handler = handler;
        // 创建一个不可变的中间件列表副本
        this.middlewares = middlewares != null ? List.copyOf(middlewares) : List.of();
    }

    // 保留旧的构造函数用于可能的其他地方调用，或者重构所有调用点
    public Route(String method, String path, Pattern pathPattern, RequestHandler handler) {
        this(method, path, pathPattern, handler, null);
    }

    // Getter for middlewares
    public List<Handler> getMiddlewares() {
        return this.middlewares;
    }
}
