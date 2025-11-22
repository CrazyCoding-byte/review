package com.yzx.web_flux_demo.net.config;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @className: Http2RequestContext
 * @author: yzx
 * @date: 2025/11/22 17:08
 * @Version: 1.0
 * @description:
 */
public class Http2RequestContext {
    private static final AttributeKey<ConcurrentHashMap<Integer, Http2Request>> REQUEST_MAP =
            AttributeKey.newInstance("http2.request.map");

    // 保存请求（流ID -> 请求对象）
    public static void setRequest(Channel channel, int streamId, Http2Request request) {
        ConcurrentHashMap<Integer, Http2Request> map = channel.attr(REQUEST_MAP).get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            channel.attr(REQUEST_MAP).set(map);
        }
        map.put(streamId, request);
    }

    // 获取请求
    public static Http2Request getRequest(Channel channel, int streamId) {
        ConcurrentHashMap<Integer, Http2Request> map = channel.attr(REQUEST_MAP).get();
        return map == null ? null : map.get(streamId);
    }

    // 移除请求（避免内存泄漏）
    public static void removeRequest(Channel channel, int streamId) {
        ConcurrentHashMap<Integer, Http2Request> map = channel.attr(REQUEST_MAP).get();
        if (map != null) {
            map.remove(streamId);
        }
    }
}
