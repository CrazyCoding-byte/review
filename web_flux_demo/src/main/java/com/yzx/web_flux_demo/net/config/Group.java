package com.yzx.web_flux_demo.net.config;

import com.yzx.web_flux_demo.net.config.core.Handler;

import java.util.ArrayList;
import java.util.List;

/**
 * @className: Group
 * @author: yzx
 * @date: 2025/11/23 1:34
 * @Version: 1.0
 * @description:
 */
public class Group {
    private final String prefix;
    private final List<Handler> middlewares; // 组级别的中间件
    private final Router router; // 持有父级 Router 的引用，用于注册路由

    public Group(String prefix, List<Handler> middlewares, Router router) {
        this.prefix = prefix != null ? prefix : "";
        this.middlewares = middlewares != null ? new ArrayList<>(middlewares) : new ArrayList<>();
        this.router = router;
    }

    /**
     * 注册 GET 请求路由
     * @param path 路径 (相对于组前缀)
     * @param handler 业务逻辑处理器
     */
    public void GET(String path, RequestHandler handler) {
        String fullPath = this.prefix + path;
        // 将组的中间件和路由特定的处理器组合
        // 这里需要一个机制将 RequestHandler 适配为 Handler，并与中间件链起来
        // 暂时直接注册到 router，但需要传递中间件信息
        // 方案：Router.register 可以接受一个 Handler 列表
        // 或者，在 Router 内部维护一个结构，将中间件与 Route 关联
        // 更简单的方式：在 Router.register 时，将组中间件和路由中间件合并
        // 这里假设 Router.register 会处理中间件合并
        router.register("GET", fullPath, handler, this.middlewares);
    }

    /**
     * 注册 POST 请求路由
     * @param path 路径 (相对于组前缀)
     * @param handler 业务逻辑处理器
     */
    public void POST(String path, RequestHandler handler) {
        String fullPath = this.prefix + path;
        router.register("POST", fullPath, handler, this.middlewares);
    }

    /**
     * 注册 PUT 请求路由
     * @param path 路径 (相对于组前缀)
     * @param handler 业务逻辑处理器
     */
    public void PUT(String path, RequestHandler handler) {
        String fullPath = this.prefix + path;
        router.register("PUT", fullPath, handler, this.middlewares);
    }

    /**
     * 注册 DELETE 请求路由
     * @param path 路径 (相对于组前缀)
     * @param handler 业务逻辑处理器
     */
    public void DELETE(String path, RequestHandler handler) {
        String fullPath = this.prefix + path;
        router.register("DELETE", fullPath, handler, this.middlewares);
    }

    // 可以添加其他 HTTP 方法 (HEAD, OPTIONS, PATCH 等)

    /**
     * 为当前组添加中间件
     * @param middleware 中间件处理器
     * @return 当前 Group 实例，支持链式调用
     */
    public Group Use(Handler middleware) {
        this.middlewares.add(middleware);
        return this; // 支持链式调用
    }

    /**
     * 创建子分组
     * @param subPrefix 子分组的前缀 (会追加在当前组前缀之后)
     * @return 新的 Group 实例
     */
    public Group Group(String subPrefix) {
        // 将当前组的前缀和中间件传递给子组
        return new Group(this.prefix + subPrefix, this.middlewares, this.router);
    }
}
}
