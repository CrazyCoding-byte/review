package com.yzx.web_flux_demo.net.config.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @className: Http1Context
 * @author: yzx
 * @date: 2025/11/23 0:36
 * @Version: 1.0
 * @description:
 */
public class Http1Context implements Context {

    private final ChannelHandlerContext nettyCtx;
    private final io.netty.handler.codec.http.FullHttpRequest nettyRequest; // Netty 原始请求
    private final Http1Request http1Request; // 您定义的封装了 FullHttpRequest 的 Request
    private final Http1Response http1Response; // 我们之前定义的 Response 实现
    private final HandlerChain handlerChain; // 新的 Handler 链

    // 用于存储请求范围内的数据
    private final Map<String, Object> attributes = new HashMap<>();

    // 用于 JSON 序列化
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Http1Context(ChannelHandlerContext nettyCtx, io.netty.handler.codec.http.FullHttpRequest nettyRequest, Http1Request http1Request, Http1Response http1Response, HandlerChain handlerChain) {
        this.nettyCtx = nettyCtx;
        this.nettyRequest = nettyRequest;
        this.http1Request = http1Request;
        this.http1Response = http1Response;
        this.handlerChain = handlerChain;
    }

    @Override
    public Request request() {
        // 返回您定义的 Request 实现
        return http1Request;
    }

    @Override
    public Response response() { // 注意：这里返回的是您 config 包下的 Response 接口
        // 由于 Http1Response 实现了 com.yzx.web_flux_demo.net.config.Response
        // 但 Context 接口返回的是 com.yzx.web_flux_demo.net.config.Request
        // 这里需要一个适配器或者重新思考 Response 接口的归属
        // 暂时返回 Http1Response，但类型转换可能需要调整
        // 为了继续，我们假设 config 包下的 Response 接口是统一的
        // 否则，需要创建一个实现 com.yzx.web_flux_demo.net.core.Response 的适配器
        // 这里先按最直接的方式处理，如果您的 config.Response 不是 core.Response，则需要适配
        return http1Response;
    }

    @Override
    public String method() {
        return nettyRequest.method().name();
    }

    @Override
    public String path() {
        return nettyRequest.uri(); // 可以进一步解析，去除查询参数
    }

    @Override
    public String queryParam(String key) {
        // 使用 Netty 的 QueryStringDecoder 解析查询参数
        QueryStringDecoder decoder = new QueryStringDecoder(nettyRequest.uri());
        return decoder.parameters().get(key).stream().findFirst().orElse(null);
    }

    @Override
    public String pathParam(String key) {
        // 从 http1Request 中获取由路由层填充的路径参数
        return http1Request.pathParams().get(key);
    }

    @Override
    public <T> T bind(Class<T> clazz) {
        try {
            byte[] bodyBytes = http1Request.body();
            if (bodyBytes != null && bodyBytes.length > 0) {
                // 假设请求体是 JSON
                return objectMapper.readValue(bodyBytes, clazz);
            }
        } catch (Exception e) {
            System.err.println("Failed to bind request body to class " + clazz.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void status(int code) {
        http1Response.status(io.netty.handler.codec.http.HttpResponseStatus.valueOf(code));
    }

    @Override
    public void json(Object obj) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(obj);
            http1Response.setHeader("Content-Type", "application/json; charset=UTF-8");
            http1Response.setBody(jsonBytes);
        } catch (Exception e) {
            System.err.println("Failed to serialize object to JSON: " + e.getMessage());
            e.printStackTrace();
            // 设置错误状态码
            status(500);
            text("Internal Server Error");
        }
    }

    @Override
    public void html(String html) {
        http1Response.setHeader("Content-Type", "text/html; charset=UTF-8");
        http1Response.setBody(html.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void text(String text) {
        http1Response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        http1Response.setBody(text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void next() {
        // 调用 HandlerChain 的 next 方法
        handlerChain.next(this);
    }

    @Override
    public void set(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object value = attributes.get(key);
        if (value != null && clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    // 提供获取原始 Netty 对象的方法，以备不时之需
    public ChannelHandlerContext getNettyCtx() {
        return nettyCtx;
    }

    public io.netty.handler.codec.http.FullHttpRequest getNettyRequest() {
        return nettyRequest;
    }
}