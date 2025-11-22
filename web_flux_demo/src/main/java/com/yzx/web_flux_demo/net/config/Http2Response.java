package com.yzx.web_flux_demo.net.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Data;
import lombok.Getter;

/**
 * @className: Http2Response
 * @author: yzx
 * @date: 2025/11/22 17:02
 * @Version: 1.0
 * @description:
 */
/**
 * HTTP/2 响应对象封装
 */
@Getter
public class Http2Response {
    private final HttpResponseStatus status; // 响应状态码
    private final String contentType; // 内容类型（如 application/json）
    private final ByteBuf body; // 响应体（Netty缓冲区，供底层发送使用）
    private final int bodyLength; // 响应体长度

    // 1. 业务层常用：接受字节数组（最推荐使用）
    public Http2Response(HttpResponseStatus status, String contentType, byte[] bodyBytes) {
        this.status = status;
        this.contentType = contentType;
        this.body = Unpooled.wrappedBuffer(bodyBytes); // 包装字节数组为ByteBuf（零拷贝）
        this.bodyLength = bodyBytes.length;
    }

    // 2. 底层IO层使用：直接接受ByteBuf
    public Http2Response(HttpResponseStatus status, String contentType, ByteBuf body) {
        this.status = status;
        this.contentType = contentType;
        this.body = body;
        this.bodyLength = body.readableBytes(); // 从ByteBuf中获取实际长度
    }
}
