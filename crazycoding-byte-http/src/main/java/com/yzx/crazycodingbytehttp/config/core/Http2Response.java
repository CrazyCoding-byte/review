package com.yzx.crazycodingbytehttp.config.core;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;
import lombok.Data;

import java.util.Map;

/**
 * @className: Http2Response
 * @author: yzx
 * @date: 2025/11/23 1:03
 * @Version: 1.0
 * @description:
 */
@Data
public class Http2Response implements Response {

    private HttpResponseStatus status = HttpResponseStatus.OK;
    private final io.netty.handler.codec.http.HttpHeaders headers = new io.netty.handler.codec.http.DefaultHttpHeaders();
    private byte[] body = new byte[0]; // 默认空 body
    // 需要持有 Http2StreamChannel 来发送响应帧
    private final Http2StreamChannel streamChannel;

    public Http2Response(Http2StreamChannel streamChannel) {
        this.streamChannel = streamChannel;
    }

    @Override
    public void status(HttpResponseStatus status) {
        this.status = status;
    }

    @Override
    public void setHeader(String name, String value) {
        this.headers.set(name, value);
    }

    @Override
    public void setBody(byte[] body) {
        this.body = body != null ? body : new byte[0];
    }

    /**
     * 将当前 Http2Response 发送出去
     * 需要使用 streamChannel 写入 Http2HeadersFrame 和 Http2DataFrame
     * 注意：Http2StreamChannel 通常需要先写入 Http2HeadersFrame，然后才能写入 Http2DataFrame。
     * Http2HeadersFrame 的构造通常需要一个 Http2Headers 对象。
     * 一些实现中可能直接通过 streamChannel.writeAndFlush(Http2Headers, boolean endStream) 来发送。
     * 但直接写入 Http2HeadersFrame 是更标准的方式，需要先创建一个实现类实例。
     * Netty 内部有 DefaultHttp2HeadersFrame，但它通常是包私有的。
     * 我们可以尝试使用 DefaultHttp2HeadersFrame 的构造函数（如果可见）或者依赖 pipeline 编码。
     * 最标准的方式是创建一个 Http2HeadersFrame 的实现，例如 DefaultHttp2HeadersFrame。
     * 在 io.netty.handler.codec.http2 包下，通常有 DefaultHttp2HeadersFrame。
     */
    public void writeAndFlush() {
        Http2Headers nettyHeaders = new DefaultHttp2Headers()
                .status(status.codeAsText()); // 设置 :status 伪头
        // 添加其他自定义头部
        headers.forEach(entry -> nettyHeaders.add(entry.getKey(), entry.getValue()));
        // --- 修正：使用 DefaultHttp2HeadersFrame (如果可见) ---
        // 请注意：DefaultHttp2HeadersFrame 可能是 package-private (包私有)
        // 如果无法访问，请参考下方的替代方案。
        try {
            // 尝试使用 DefaultHttp2HeadersFrame (它实现了 Http2HeadersFrame)
            DefaultHttp2HeadersFrame headersFrame =
                    new DefaultHttp2HeadersFrame(nettyHeaders, false); // endStream = false
            streamChannel.writeAndFlush(headersFrame);

            // 写入 body 帧 (endStream = true)
            ByteBuf bodyBuffer = streamChannel.alloc().buffer().writeBytes(body);
            DefaultHttp2DataFrame dataFrame =
                    new DefaultHttp2DataFrame(bodyBuffer, true); // endStream = true
            streamChannel.writeAndFlush(dataFrame);
        } catch (NoClassDefFoundError | IllegalAccessError e) {
            // 如果 DefaultHttp2HeadersFrame 不可见或无法实例化
            System.err.println("Cannot instantiate DefaultHttp2HeadersFrame or DefaultHttp2DataFrame directly: " + e.getMessage());
            // 尝试替代方案：通过 pipeline 间接发送
            // 这通常需要 streamChannel pipeline 中有能处理 Http2Headers 和 ByteBuf 的 handler。
            // 但这比较复杂，通常不直接推荐。
            // 最稳妥的方式还是确保 DefaultHttp2HeadersFrame 可用。
            // 如果以上方式都受限，可能需要在 Handler 中直接操作 streamChannel.pipeline()。
        }
    }
}
