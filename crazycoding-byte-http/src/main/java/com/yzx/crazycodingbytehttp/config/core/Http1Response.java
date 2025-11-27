package com.yzx.crazycodingbytehttp.config.core;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.Data;

/**
 * @className: Http1Response
 * @author: yzx
 * @date: 2025/11/23 1:02
 * @Version: 1.0
 * @description:
 */
@Data
public class Http1Response implements Response { // 实现 config 包下的 Response

    private HttpResponseStatus status = HttpResponseStatus.OK;
    private final io.netty.handler.codec.http.HttpHeaders headers = new io.netty.handler.codec.http.DefaultHttpHeaders();
    private byte[] body = new byte[0]; // 默认空 body

    @Override
    public void status(HttpResponseStatus status) { // 实现接口方法
        this.status = status;
    }

    @Override
    public void setHeader(String name, String value) { // 实现接口方法
        this.headers.set(name, value);
    }

    @Override
    public void setBody(byte[] body) { // 实现接口方法
        this.body = body != null ? body : new byte[0];
    }

    /**
     * 将当前 Http1Response 转换为 Netty 的 FullHttpResponse
     * @return FullHttpResponse
     */
    public FullHttpResponse toNettyResponse() {
        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                io.netty.buffer.Unpooled.wrappedBuffer(body)
        );
        nettyResponse.headers().add(headers);
        nettyResponse.headers().setInt("Content-Length", body.length); // 设置 Content-Length
        return nettyResponse;
    }
}