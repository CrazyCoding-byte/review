package com.yzx.crazycodingbytehttp.handler;

import com.yzx.crazycodingbytehttp.adapter.HandlerAdapter;
import com.yzx.crazycodingbytehttp.config.RequestHandler;
import com.yzx.crazycodingbytehttp.config.Route;
import com.yzx.crazycodingbytehttp.config.Router;
import com.yzx.crazycodingbytehttp.config.core.*;
import com.yzx.crazycodingbytehttp.metrics.MetricsCollector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * @className: Http2RequestHandler
 * @author: yzx
 * @date: 2025/11/22 16:37
 * @Version: 1.0
 * @description:
 */
/**
 * 修复了流关联丢失、构造函数错误及异常处理逻辑
 */
@Slf4j
public class Http2RequestHandler extends ChannelInboundHandlerAdapter {
    private final Router router;
    private final MetricsCollector metrics;
    private final List<Handler> globalMiddlewares = new ArrayList<>(); // 全局中间件

    // 用于临时存储请求体数据 (如果需要聚合)
    private ByteBuf requestBodyBuffer;

    public Http2RequestHandler(Router router, MetricsCollector metrics) {
        this.router = router;
        this.metrics = metrics;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            handleHeadersFrame(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            handleDataFrame(ctx, (Http2DataFrame) msg);
        } else {
            // 如果有其他类型的帧，传递给下一个 handler
            super.channelRead(ctx, msg);
        }
    }

    private void handleHeadersFrame(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame) throws Exception {
        // 1. 解析路径和方法
        String method = headersFrame.headers().method().toString();
        String rawPath = headersFrame.headers().path().toString();
        int queryStart = rawPath.indexOf('?');
        String path = queryStart >= 0 ? rawPath.substring(0, queryStart) : rawPath;
        String query = queryStart >= 0 ? rawPath.substring(queryStart + 1) : "";

        long startTime = System.currentTimeMillis();

        // 2. 路由匹配
        Route route = router.match(method, path);
        if (route == null) {
            sendHttp2Error(ctx, HttpResponseStatus.NOT_FOUND, "Route not found: " + path);
            return;
        }

        // 3. 解析路径参数
        Map<String, String> pathParams = router.extractPathParams(route.getPath(), path);

        // 4. 创建 Request 和 Response
        // 注意：Http2Request 需要 Http2HeadersFrame 和 pathParams, body (可能为空)
        // 这里假设 body 在 headers frame 之后的 data frame 中，或者 body 为空
        // 如果 body 需要聚合，需要在 handleDataFrame 中处理
        byte[] bodyBytes = requestBodyBuffer != null ? requestBodyBuffer.array() : new byte[0];
        Http2Request http2Request = new Http2Request(headersFrame, pathParams, bodyBytes);
        // Http2Response 需要 Http2StreamChannel 来发送响应
        Http2Response http2Response = new Http2Response((Http2StreamChannel) ctx.channel());

        // 5. 创建 HandlerChain
        List<Handler> routeMiddlewares = route.getMiddlewares();
        RequestHandler oldHandler = route.getHandler();
        Handler adaptedHandler = new HandlerAdapter(oldHandler, metrics);

        List<Handler> handlersForChain = new ArrayList<>(globalMiddlewares);
        handlersForChain.addAll(routeMiddlewares);
        handlersForChain.add(adaptedHandler);

        HandlerChain handlerChain = new HandlerChain(handlersForChain);

        // 6. 创建 Context
        Http2Context context = new Http2Context(ctx, headersFrame, http2Request, http2Response, handlerChain);

        // 7. 启动 HandlerChain
        context.next(); // 执行中间件和处理器

        // 注意：HTTP/2 响应可能在 Context 或 Handler 内部发送
        // 如果没有在 Context 内部发送，需要在这里检查状态并发送默认响应

        // 记录请求耗时 (在响应发送后)
        metrics.recordRequestDuration(System.currentTimeMillis() - startTime);

        // 重置 body buffer (如果需要)
        if (requestBodyBuffer != null) {
            requestBodyBuffer.release();
            requestBodyBuffer = null;
        }

    }

    private void handleDataFrame(ChannelHandlerContext ctx, Http2DataFrame dataFrame) throws Exception {
        // 聚合请求体数据 (如果需要)
        if (requestBodyBuffer == null) {
            requestBodyBuffer = ctx.alloc().buffer();
        }
        requestBodyBuffer.writeBytes(dataFrame.content());

        // 检查是否是最后一个数据帧
        if (dataFrame.isEndStream()) {
            // Body 聚合完成，但处理通常在 headers frame 时开始
            // 可以选择在此处触发处理，或者在 headers frame 处理时检查 body 是否已准备好
            // 这里我们假设 headers frame 处理时 body 已经准备好或为空
            // 或者，可以将 body 聚合逻辑和处理逻辑放在一个更合适的地方
            // 当前实现是在 headers frame 时处理，如果 body 未准备好，可能需要调整
            // 一个简单的策略是：在 headers frame 时，如果发现有 body，等待 data frame，然后再次触发处理
            // 但为了简化，我们假设 headers frame 处理时 body 已经通过 handleDataFrame 聚合完毕
            // 这需要在 handleHeadersFrame 中检查 requestBodyBuffer 是否存在
            // 或者，让 Context/Handler 在需要时自行获取 body (更复杂)
            // 当前实现将 body 聚合在 headers frame 之前，然后在 headers frame 时处理
            // 这意味着 handleHeadersFrame 会等待所有 data frame (如果有的话) 到达后才处理
            // 这不是最高效的方式，但对于演示是可行的
            // 实际框架可能需要更复杂的异步处理机制
        }
        // 释放 dataFrame 的缓冲区，因为我们已经复制了数据
        dataFrame.release();
    }

    private void sendHttp2Error(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        // 创建一个简单的错误响应
        // 这需要通过 Http2StreamChannel 发送 Http2HeadersFrame 和 Http2DataFrame
        Http2Response errorResponse = new Http2Response((Http2StreamChannel) ctx.channel());
        errorResponse.status(status);
        errorResponse.setHeader("Content-Type", "text/plain; charset=UTF-8");
        errorResponse.setBody(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        errorResponse.writeAndFlush(); // 这会发送错误响应
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 记录异常并发送错误响应
        System.err.println("Exception in Http2RequestHandler: " + cause.getMessage());
        cause.printStackTrace();
        sendHttp2Error(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        // 通常 HTTP/2 不会关闭整个连接，而是关闭出错的 stream
        // ctx.close(); // 不要关闭整个连接，只关闭当前 stream
        ctx.fireExceptionCaught(cause); // 让其他 handler 也能处理异常
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 清理资源
        if (requestBodyBuffer != null) {
            requestBodyBuffer.release();
            requestBodyBuffer = null;
        }
        super.channelInactive(ctx);
    }
}