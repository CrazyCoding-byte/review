package com.yzx.web_flux_demo.net.handler;

import com.yzx.web_flux_demo.net.adapter.HandlerAdapter;
import com.yzx.web_flux_demo.net.config.*;
import com.yzx.web_flux_demo.net.config.core.*;
import com.yzx.web_flux_demo.net.metrics.MetricsCollector;
import com.yzx.web_flux_demo.net.server.Http2Server;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @className: Http1RequestHandler
 * @author: yzx
 * @date: 2025/11/22 21:44
 * @Version: 1.0
 * @description:
 */
public class Http1RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final Router router;
    private final MetricsCollector metrics;
    // 假设这里可以注入或配置全局中间件
    private final List<Handler> globalMiddlewares = new ArrayList<>();

    public Http1RequestHandler(Router router, MetricsCollector metrics) {
        this.router = router;
        this.metrics = metrics;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendHttp1Error(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid request");
            return;
        }

        String method = request.method().name();
        String uri = request.uri();
        // 解析路径部分，不包含查询参数
        int queryStart = uri.indexOf('?');
        String path = queryStart >= 0 ? uri.substring(0, queryStart) : uri;
        long startTime = System.currentTimeMillis();

        // 1. 使用 Router 进行匹配
        Route route = router.match(method, path);
        if (route == null) {
            sendHttp1Error(ctx, HttpResponseStatus.NOT_FOUND, "Route not found: " + path);
            return;
        }

        // 2. 解析路径参数
        Map<String, String> pathParams = router.extractPathParams(route.getPath(), path);

        // 3. 创建 Request 和 Response 实现
        Http1Request http1Request = new Http1Request(request, pathParams);
        Http1Response http1Response = new Http1Response(); // 创建新的响应实例

        // 4. 创建 Context
        // 注意：这里需要 HandlerChain，但我们还没有构建它
        // 我们需要先获取 Route 中的 RequestHandler，并将其适配为 HandlerChain 可用的 Handler

        // 5. 创建适配器，将旧的 RequestHandler 适配为新的 Handler
        RequestHandler oldHandler = route.getHandler();
        Handler adaptedHandler = new HandlerAdapter(oldHandler, metrics);

        // 6. 构建 HandlerChain (包含全局中间件和最终适配后的 Handler)
        // 这里需要一个包含所有中间件和最终处理器的列表
        // 暂时只包含全局中间件和适配后的最终处理器
        List<Handler> handlersForChain = new ArrayList<>(globalMiddlewares);
        handlersForChain.add(adaptedHandler);
        HandlerChain handlerChain = new HandlerChain(handlersForChain, adaptedHandler);

        // 7. 创建 Context，并传入 HandlerChain
        Http1Context context = new Http1Context(ctx, request, http1Request, http1Response, handlerChain);

        // 8. 启动 HandlerChain (从第一个中间件或最终处理器开始)
        context.next(); // 这将启动链的执行

        // 9. 发送响应
        // 注意：响应的发送应该在 HandlerChain 执行完毕后进行
        // 但是，如果 HandlerChain 中有异步操作，或者中间件调用了 next() 但没有立即完成，
        // 那么在 channelRead0 末尾发送响应可能不准确。
        // 一个更健壮的方法是：
        // - 在 Context 中添加一个标志，表示响应是否已发送。
        // - 或者，让 HandlerChain 在执行完毕后，通过 Context 调用一个发送响应的方法。
        // - 或者，在一个出站处理器中处理响应发送。
        // 为了简化，我们假设 HandlerChain 在 next() 调用后就完成了所有同步处理。
        // 检查 Connection 头
        String requestConnectionHeader = request.headers().get(HttpHeaderNames.CONNECTION);
        boolean closeConnection = HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(requestConnectionHeader) ||
                http1Response.getStatus().code() >= 400; // 示例：根据状态码决定

        if (closeConnection) {
            http1Response.setHeader(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.CLOSE.toString());
            // 发送响应并关闭连接
            ctx.writeAndFlush(http1Response.toNettyResponse()).addListener(ChannelFutureListener.CLOSE);
        } else {
            // 保持连接，发送响应
            // http1Response.setHeader(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString()); // 可选
            ctx.writeAndFlush(http1Response.toNettyResponse());
        }

        // 记录请求耗时 (在响应发送后)
        metrics.recordRequestDuration(System.currentTimeMillis() - startTime);
    }

    // 发送 HTTP/1.1 错误响应
    private void sendHttp1Error(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        io.netty.handler.codec.http.FullHttpResponse response = new io.netty.handler.codec.http.DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.wrappedBuffer(bytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
