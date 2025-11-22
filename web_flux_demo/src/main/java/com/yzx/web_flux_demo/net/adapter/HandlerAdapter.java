package com.yzx.web_flux_demo.net.adapter;

import com.yzx.web_flux_demo.net.config.Http2Response;
import com.yzx.web_flux_demo.net.config.Http2ResponseCallback;
import com.yzx.web_flux_demo.net.config.RequestHandler;
import com.yzx.web_flux_demo.net.config.core.Context;
import com.yzx.web_flux_demo.net.config.core.Handler;
import com.yzx.web_flux_demo.net.config.core.Request;
import com.yzx.web_flux_demo.net.config.core.Response;
import com.yzx.web_flux_demo.net.metrics.MetricsCollector;
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
    private final MetricsCollector metrics; // 可选：如果需要在适配器中记录指标

    public HandlerAdapter(RequestHandler oldHandler, MetricsCollector metrics) {
        this.oldHandler = oldHandler;
        this.metrics = metrics;
    }

    @Override
    public void handle(Context context) throws Exception {
        // 获取 Context 中封装的 Request 和 Response
        Request request = context.request();
        Response response = (Response) context.response(); // 注意：这里假设 response 是 config 包下的 Response

        long startTime = System.currentTimeMillis();

        // 调用旧的 RequestHandler
        oldHandler.handle(request, new Http2ResponseCallback() { // 这里使用了旧的 Callback，需要适配
            @Override
            public void onSuccess(Http2Response http2Response) {
                // 将 Http2Response 的数据复制到 Context 的 Response 中
                // 这需要 Http1Response 和 Http2Response 有共同的抽象或适配
                // 为了简化，假设 Response 接口有 setBody 和 setHeader 方法
                // 并且 Context.response() 返回的是 com.yzx.web_flux_demo.net.config.Response
                // context.response().setBody(http2Response.getBody()); // 这行可能需要适配
                // context.response().setHeader("Content-Type", http2Response.getContentType()); // 这行可能需要适配
                // 直接使用 Context 的方法设置响应
                context.status(http2Response.getStatus().code());
                context.response().setBody(http2Response.getBody().array());
                context.response().setHeader("Content-Type", http2Response.getContentType());

                if (metrics != null) {
                    metrics.recordRequestDuration(System.currentTimeMillis() - startTime);
                }
            }

            @Override
            public void onFailure(Throwable cause) {
                // 设置错误响应
                context.status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                context.text("Internal Server Error: " + cause.getMessage());
                if (metrics != null) {
                    metrics.incrementErrorCount();
                }
            }
        });
    }
}
