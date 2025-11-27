package com.yzx.crazycodingbytehttp.config.core;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @className: Http2Request
 * @author: yzx
 * @date: 2025/11/23 0:54
 * @Version: 1.0
 * @description:
 */
@Data
public class Http2Request implements Request {
    // 在实际 HTTP/2 实现中，你可能持有 Http2HeadersFrame 或 Http2StreamChannel 的引用
    // 这里简化为直接持有解析后的数据
    private final HttpMethod method;
    private final String path;
    private final String query;
    private final Map<String, List<String>> queryParams;
    private final Map<String, String> pathParams; // 由路由层填充
    private final Map<String, String> headers;
    private final byte[] body; // 假设 body 已经被聚合
    private final String protocolVersionStr; // HTTP/2 协议名称

    public Http2Request(Http2HeadersFrame headersFrame, Map<String, String> pathParams, byte[] body) {
        Http2Headers nettyHeaders = headersFrame.headers();
        this.method = HttpMethod.valueOf(nettyHeaders.method().toString());
        String rawPath = nettyHeaders.path().toString();
        // 解析路径和查询参数
        int queryStart = rawPath.indexOf('?');
        this.path = queryStart >= 0 ? rawPath.substring(0, queryStart) : rawPath;
        this.query = queryStart >= 0 ? rawPath.substring(queryStart + 1) : "";

        QueryStringDecoder decoder = new QueryStringDecoder(rawPath);
        this.queryParams = decoder.parameters();

        this.pathParams = pathParams != null ? pathParams : new HashMap<>();

        // 将 Http2Headers 转换为 Map<String, String>
        // 注意：HTTP/2 headers 可能包含多个同名 header，这里简化处理，只取第一个值
        Map<String, String> tempHeaders = new HashMap<>();
        for (Map.Entry<CharSequence, CharSequence> entry : nettyHeaders) {
            tempHeaders.put(entry.getKey().toString(), entry.getValue().toString());
        }
        this.headers = tempHeaders;

        this.body = body != null ? body : new byte[0];

        // 设置 HTTP/2 协议名称
        this.protocolVersionStr = Http2CodecUtil.TLS_UPGRADE_PROTOCOL_NAME.toString(); // 值为 "h2"
    }

    @Override
    public HttpMethod method() {
        return method;
    }

    @Override
    public String protocolVersion() {
        return protocolVersionStr; // 返回 "h2"
    }

    @Override
    public String uri() {
        // HTTP/2 中没有 URI，只有 :path, :method, :scheme, :authority 伪头
        // 这里返回一个模拟的 URI
        return path + (query.isEmpty() ? "" : "?" + query);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String query() {
        return query;
    }

    @Override
    public Map<String, List<String>> queryParams() {
        return queryParams;
    }

    @Override
    public Map<String, String> pathParams() {
        return pathParams;
    }

    @Override
    public byte[] body() {
        return body;
    }

    @Override
    public String header(String name) {
        return headers.get(name);
    }
}
