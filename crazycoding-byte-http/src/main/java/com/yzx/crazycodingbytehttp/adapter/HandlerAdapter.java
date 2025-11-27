package com.yzx.crazycodingbytehttp.adapter;

import com.yzx.crazycodingbytehttp.config.Http2ResponseCallback;
import com.yzx.crazycodingbytehttp.config.RequestHandler;
import com.yzx.crazycodingbytehttp.config.core.Context;
import com.yzx.crazycodingbytehttp.config.core.Handler;
import com.yzx.crazycodingbytehttp.config.core.Http2Response;
import com.yzx.crazycodingbytehttp.config.core.Response;
import com.yzx.crazycodingbytehttp.metrics.MetricsCollector;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @className: HandlerAdapter
 * @author: yzx
 * @date: 2025/11/23 0:45
 * @Version: 1.0
 * @description:
 */
public class HandlerAdapter implements Handler {
    private final RequestHandler oldHandler;
    private final MetricsCollector metrics;

    public HandlerAdapter(RequestHandler oldHandler, MetricsCollector metrics) {
        this.oldHandler = oldHandler;
        this.metrics = metrics;
    }

    @Override
    public void handle(Context context) throws Exception { // 实现 Handler.handle 方法
        long startTime = System.currentTimeMillis();

        // 调用旧的 RequestHandler
        oldHandler.handle(context.request(), new Http2ResponseCallback() {
            @Override
            public void onSuccess(Http2Response http2Response) {
                Response contextResponse = context.response();
                contextResponse.status(http2Response.getStatus());
                contextResponse.setHeader("Content-Type", "text/plain;charset=UTF-8");
                contextResponse.setBody(http2Response.getBody());

                if (metrics != null) {
                    metrics.recordRequestDuration(System.currentTimeMillis() - startTime);
                }
            }

            @Override
            public void onFailure(Throwable cause) {
                context.status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                context.text("Internal Server Error: " + cause.getMessage());
                if (metrics != null) {
                    metrics.incrementErrorCount();
                }
            }
        });
    }
}
