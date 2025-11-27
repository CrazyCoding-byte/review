package com.yzx.crazycodingbytehttp.config.core;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.util.List;
import java.util.Map;

/**
 * @className: Request
 * @author: yzx
 * @date: 2025/11/23 0:35
 * @Version: 1.0
 * @description:
 */
public interface Request {
    HttpMethod method();
    String protocolVersion();
    String uri();
    String path();
    String query();
    // 注意：这里 queryParams 的返回类型也与您之前提供的不一致，我们保持 List<String>
    Map<String, List<String>> queryParams(); // 修改为 List<String>
    Map<String, String> pathParams(); // 由路由层填充
    byte[] body(); // 或者返回 ByteBuf
    String header(String name);
    // ... 其他需要的请求信息方法
}
