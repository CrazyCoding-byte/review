package com.yzx.web_flux_demo.net.config.core;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @className: Response
 * @author: yzx
 * @date: 2025/11/23 0:35
 * @Version: 1.0
 * @description:
 */
public interface Response {
    void status(HttpResponseStatus status);
    void setHeader(String name, String value);
    void setBody(byte[] body); // 或者接受 ByteBuf
    // ... 其他需要的响应操作方法
}
