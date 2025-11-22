package com.yzx.web_flux_demo.net.config.core;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @className: Http1Request
 * @author: yzx
 * @date: 2025/11/23 0:53
 * @Version: 1.0
 * @description:
 */
@Data
public class Http1Request implements Request {

    private final FullHttpRequest nettyRequest;
    private final Map<String, String> pathParams; // 由路由层填充
    // 缓存解析结果，避免重复解析
    private Map<String, List<String>> queryParams;
    private String path;
    private String query;
    private String protocolVersionStr; // 缓存协议版本字符串
    public Http1Request(FullHttpRequest nettyRequest, Map<String, String> pathParams) {
        this.nettyRequest = nettyRequest;
        this.pathParams = pathParams != null ? pathParams : new HashMap<>();
    }

    @Override
    public HttpMethod method() {
        return nettyRequest.method();
    }

    @Override
    public String protocolVersion() {
        if (protocolVersionStr == null) {
            // 将 HttpVersion 转换为字符串表示
            protocolVersionStr = nettyRequest.protocolVersion().text();
        }
        return protocolVersionStr;
    }

    @Override
    public String uri() {
        return nettyRequest.uri();
    }

    @Override
    public String path() {
        if (path == null) {
            // 从 URI 中解析路径部分 (不包含查询参数)
            int queryStart = nettyRequest.uri().indexOf('?');
            path = queryStart >= 0 ? nettyRequest.uri().substring(0, queryStart) : nettyRequest.uri();
        }
        return path;
    }

    @Override
    public String query() {
        if (query == null) {
            // 从 URI 中解析查询参数部分
            int queryStart = nettyRequest.uri().indexOf('?');
            query = queryStart >= 0 ? nettyRequest.uri().substring(queryStart + 1) : "";
        }
        return query;
    }

    @Override
    public Map<String, List<String>> queryParams() {
        if (queryParams == null) {
            // 使用 Netty 的 QueryStringDecoder 解析查询参数
            QueryStringDecoder decoder = new QueryStringDecoder(nettyRequest.uri());
            queryParams = decoder.parameters();
        }
        return queryParams;
    }

    @Override
    public Map<String, String> pathParams() {
        return pathParams;
    }

    @Override
    public byte[] body() {
        ByteBuf content = nettyRequest.content();
        byte[] body = new byte[content.readableBytes()];
        content.readBytes(body);
        // 注意：这里释放了 content 的引用计数。调用者需要确保不再使用 nettyRequest.content()
        // 如果需要多次访问 body，可以考虑在构造函数中就将其复制出来。
        ReferenceCountUtil.release(content);
        return body;
    }

    @Override
    public String header(String name) {
        return nettyRequest.headers().get(name);
    }

    // 提供获取原始 Netty 请求对象的方法，以备不时之需
    public FullHttpRequest getNettyRequest() {
        return nettyRequest;
    }
}
