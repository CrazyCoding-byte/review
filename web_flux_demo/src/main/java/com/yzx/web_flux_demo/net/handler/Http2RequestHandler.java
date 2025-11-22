package com.yzx.web_flux_demo.net.handler;

import com.yzx.web_flux_demo.net.config.*;
import com.yzx.web_flux_demo.net.metrics.MetricsCollector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

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

    public Http2RequestHandler(Router router, MetricsCollector metrics) {
        this.router = router;
        this.metrics = metrics;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Http2FrameStream currentStream = (msg instanceof Http2StreamFrame) ? ((Http2StreamFrame) msg).stream() : null;
        try {
            if (msg instanceof Http2HeadersFrame) {
                handleHeadersFrame(ctx, (Http2HeadersFrame) msg);
            } else if (msg instanceof Http2DataFrame) {
                handleDataFrame(ctx, (Http2DataFrame) msg);
            } else if (msg instanceof Http2PingFrame) {
                handlePingFrame(ctx, (Http2PingFrame) msg);
            } else if (msg instanceof Http2SettingsFrame) {
                handleSettingsFrame(ctx, (Http2SettingsFrame) msg);
            } else if (msg instanceof Http2ResetFrame) {
                handleResetFrame(ctx, (Http2ResetFrame) msg);
            } else {
                log.warn("Unhandled HTTP/2 frame type: {}", msg.getClass().getSimpleName());
                ReferenceCountUtil.release(msg);
            }
        } catch (Exception e) {
            log.error("Error handling HTTP/2 frame", e);
            metrics.incrementErrorCount();
            if (currentStream != null) {
                sendResetFrame(ctx, currentStream, Http2Error.INTERNAL_ERROR.code());
            } else {
                ctx.close();
            }
        }
    }

    private void handleHeadersFrame(ChannelHandlerContext ctx, Http2HeadersFrame frame) {
        Http2Headers headers = frame.headers();
        Http2FrameStream stream = frame.stream();
        int streamId = stream.id();
        String method = headers.method().toString();
        String path = headers.path().toString();

        log.info("Received HEADERS: streamId={}, method={}, path={}", streamId, method, path);

        Http2Request request = new Http2Request(headers, streamId, ctx.channel());
        Http2RequestContext.setRequest(ctx.channel(), streamId, request);

        // 1. 查找匹配的路由
        Route route = router.match(method, path);
        if (route == null) {
            sendErrorResponse(ctx, stream, HttpResponseStatus.NOT_FOUND, "Route not found");
            return;
        }

        // 2. 解析路径参数
        Map<String, String> pathParams = router.extractPathParams(route.getPath(), path);
        request.setPathParams(pathParams);

        // 如果是 END_STREAM，意味着没有 DATA 帧了，可以直接处理
        if (frame.isEndStream()) {
            dispatchToBusinessLogic(ctx, stream, request, route.getHandler());
        }
    }

    private void handleDataFrame(ChannelHandlerContext ctx, Http2DataFrame frame) {
        Http2FrameStream stream = frame.stream();
        int streamId = stream.id();
        ByteBuf content = frame.content();

        log.info("Received DATA: streamId={}, length={}, endStream={}", streamId, content.readableBytes(), frame.isEndStream());

        Http2Request request = Http2RequestContext.getRequest(ctx.channel(), streamId);
        if (request == null) {
            log.warn("Received DATA for unknown streamId={}", streamId);
            ReferenceCountUtil.release(frame);
            return;
        }

        // 聚合请求体
        ByteBuf bodyBuf = ctx.alloc().buffer();
        if (request.getBody() != null) {
            bodyBuf.writeBytes(request.getBody());
        }
        bodyBuf.writeBytes(content);
        request.setBody(bodyBuf.array());
        bodyBuf.release();

        ReferenceCountUtil.release(frame);

        // 如果是最后一个 DATA 帧，就可以处理请求了
        if (frame.isEndStream()) {
            Route route = router.match(request.getHeaders().method().toString(), request.getHeaders().path().toString());
            if (route != null) {
                dispatchToBusinessLogic(ctx, stream, request, route.getHandler());
            }
            // else: route not found should have been handled in HEADERS
        }
    }

    /**
     * 将请求分发给具体的业务逻辑处理器。
     */
    private void dispatchToBusinessLogic(ChannelHandlerContext ctx, Http2FrameStream stream, Http2Request request, RequestHandler handler) {
        long startTime = System.currentTimeMillis();

        // 调用业务逻辑
        handler.handle(request, new Http2ResponseCallback() {
            @Override
            public void onSuccess(Http2Response response) {
                sendSuccessResponse(ctx, stream, response);
                metrics.incrementRequestCount();
                metrics.recordRequestDuration(System.currentTimeMillis() - startTime);
                Http2RequestContext.removeRequest(ctx.channel(), stream.id());
            }

            @Override
            public void onFailure(Throwable cause) {
                log.error("Business logic failed for streamId={}", stream.id(), cause);
                sendErrorResponse(ctx, stream, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause.getMessage());
                metrics.incrementErrorCount();
                Http2RequestContext.removeRequest(ctx.channel(), stream.id());
            }
        });
    }

    private void handlePingFrame(ChannelHandlerContext ctx, Http2PingFrame frame) {
        if (frame.ack()) {
            log.debug("Received PING ACK");
        } else {
            log.debug("Received PING, sending ACK");
            ctx.writeAndFlush(new DefaultHttp2PingFrame(frame.content(), true));
        }
        ReferenceCountUtil.release(frame);
    }

    private void handleSettingsFrame(ChannelHandlerContext ctx, Http2SettingsFrame frame) {
        log.info("Received SETTINGS: {}", frame.settings());
        ctx.writeAndFlush(new DefaultHttp2SettingsFrame(Http2Settings.defaultSettings())); // Send empty ACK
        ReferenceCountUtil.release(frame);
    }

    private void handleResetFrame(ChannelHandlerContext ctx, Http2ResetFrame frame) {
        log.warn("Received RESET: streamId={}, error={}", frame.stream().id(), frame.errorCode());
        Http2RequestContext.removeRequest(ctx.channel(), frame.stream().id());
        ReferenceCountUtil.release(frame);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            log.info("Connection idle timeout, closing.");
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel exception caught", cause);
        metrics.incrementErrorCount();
        ctx.close();
    }

    private void sendSuccessResponse(ChannelHandlerContext ctx, Http2FrameStream stream, Http2Response response) {
        Http2Headers headers = new DefaultHttp2Headers()
                .status(response.getStatus().codeAsText())
                .set("Content-Type", response.getContentType())
                .setInt("Content-Length", response.getBody().toString().length());

        DefaultHttp2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, false);
        headersFrame.stream(stream);

        DefaultHttp2DataFrame dataFrame = new DefaultHttp2DataFrame(ctx.alloc().buffer().writeBytes(response.getBody()), true);
        dataFrame.stream(stream);

        ctx.write(headersFrame);
        ctx.writeAndFlush(dataFrame);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, Http2FrameStream stream, HttpResponseStatus status, String message) {
        byte[] bytes = message.getBytes();
        Http2Headers headers = new DefaultHttp2Headers()
                .status(status.codeAsText())
                .set("Content-Type", "text/plain")
                .setInt("Content-Length", bytes.length);

        DefaultHttp2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers, false);
        headersFrame.stream(stream);

        DefaultHttp2DataFrame dataFrame = new DefaultHttp2DataFrame(ctx.alloc().buffer().writeBytes(bytes), true);
        dataFrame.stream(stream);

        ctx.writeAndFlush(headersFrame);
        ctx.writeAndFlush(dataFrame);
    }

    private void sendResetFrame(ChannelHandlerContext ctx, Http2FrameStream stream, long errorCode) {
        DefaultHttp2ResetFrame resetFrame = new DefaultHttp2ResetFrame(errorCode);
        resetFrame.stream(stream);
        ctx.writeAndFlush(resetFrame);
    }
}