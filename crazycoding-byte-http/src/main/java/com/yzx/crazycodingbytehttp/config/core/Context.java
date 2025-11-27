package com.yzx.crazycodingbytehttp.config.core;



/**
 * @className: Context
 * @author: yzx
 * @date: 2025/11/23 0:32
 * @Version: 1.0
 * @description:
 */
public interface Context {
    /**
     * 获取请求对象 (来自 config 包)
     * @return Request
     */
    Request request(); // 返回 config 包下的 Request

    /**
     * 获取响应对象 (来自 config 包)
     * @return Response
     */
    Response response(); // 返回 config 包下的 Response

    // --- 请求信息获取 ---
    // 这些方法可以是对 request() 的便捷封装
    String method();
    String path();
    String queryParam(String key);
    String pathParam(String key);
    <T> T bind(Class<T> clazz); // 用于将请求体绑定到对象

    // --- 响应信息设置 ---
    // 这些方法可以是对 response() 的便捷封装
    void status(int code);
    void json(Object obj); // 设置 JSON 响应体
    void html(String html); // 设置 HTML 响应体
    void text(String text); // 设置 Text 响应体

    // --- 中间件链控制 ---
    void next(); // 执行下一个中间件或处理器

    // --- 请求范围数据存储 ---
    void set(String key, Object value);
    <T> T get(String key, Class<T> clazz);
}
