package com.yzx.web_flux_demo.net.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @className: Http2Request
 * @author: yzx
 * @date: 2025/11/22 16:59
 * @Version: 1.0
 * @description:
 */
/**
 * 封装 HTTP/2 请求的对象。
 */
public class Http2Request {
    private final Http2Headers headers;
    private final int streamId;
    private final Channel channel;
    private Map<String, String> pathParams; // 存储路径参数
    private byte[] body;

    public Http2Request(Http2Headers headers, int streamId, Channel channel) {
        this.headers = headers;
        this.streamId = streamId;
        this.channel = channel;
    }

    // --- Getters and Setters ---
    public Http2Headers getHeaders() { return headers; }
    public int getStreamId() { return streamId; }
    public Channel getChannel() { return channel; }
    public Map<String, String> getPathParams() { return pathParams; }
    public void setPathParams(Map<String, String> pathParams) { this.pathParams = pathParams; }
    public byte[] getBody() { return body; }
    public void setBody(byte[] body) { this.body = body; }
}
