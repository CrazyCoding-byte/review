package com.yzx.crazycodingbytehttp.config.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzx.web_flux_demo.net.config.core.Http2Request;
import com.yzx.web_flux_demo.net.config.core.Http2Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2HeadersFrame;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @className: Http2Context
 * @author: yzx
 * @date: 2025/11/23 1:50
 * @Version: 1.0
 * @description:
 */
public class Http2Context implements Context{
    private final ChannelHandlerContext nettyCtx; // Http2StreamChannel 的 Context
    private final Http2HeadersFrame headersFrame; // 原始请求头帧
    private final Http2Request http2Request; // 封装后的 Request 对象
    private final Http2Response http2Response; // 封装后的 Response 对象
    private final HandlerChain handlerChain; // 处理器链

    // 用于存储请求范围内的数据
    private final Map<String, Object> attributes = new HashMap<>();

    // 用于 JSON 序列化
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Http2Context(ChannelHandlerContext nettyCtx, Http2HeadersFrame headersFrame, Http2Request http2Request, Http2Response http2Response, HandlerChain handlerChain) {
        this.nettyCtx = nettyCtx;
        this.headersFrame = headersFrame;
        this.http2Request = http2Request;
        this.http2Response = http2Response;
        this.handlerChain = handlerChain;
    }

    @Override
    public Request request() {
        return http2Request;
    }

    @Override
    public Response response() {
        return http2Response;
    }

    @Override
    public String method() {
        return http2Request.method().name();
    }

    @Override
    public String path() {
        return http2Request.path();
    }

    @Override
    public String queryParam(String key) {
        List<String> values = http2Request.queryParams().get(key);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    @Override
    public String pathParam(String key) {
        return http2Request.pathParams().get(key);
    }

    @Override
    public <T> T bind(Class<T> clazz) {
        try {
            byte[] bodyBytes = http2Request.body();
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
        http2Response.status(HttpResponseStatus.valueOf(code));
    }

    @Override
    public void json(Object obj) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(obj);
            http2Response.setHeader("Content-Type", "application/json; charset=UTF-8");
            http2Response.setBody(jsonBytes);
            // 在这里发送响应
            http2Response.writeAndFlush();
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
        http2Response.setHeader("Content-Type", "text/html; charset=UTF-8");
        http2Response.setBody(html.getBytes(StandardCharsets.UTF_8));
        http2Response.writeAndFlush();
    }

    @Override
    public void text(String text) {
        http2Response.setHeader("Content-Type", "text/plain; charset=UTF-8");
        http2Response.setBody(text.getBytes(StandardCharsets.UTF_8));
        http2Response.writeAndFlush();
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

    public Http2HeadersFrame getHeadersFrame() {
        return headersFrame;
    }
}
