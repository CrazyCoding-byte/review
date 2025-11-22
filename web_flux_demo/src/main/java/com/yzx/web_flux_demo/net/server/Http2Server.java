package com.yzx.web_flux_demo.net.server;

import com.yzx.web_flux_demo.net.config.Http2Response;
import com.yzx.web_flux_demo.net.config.Router;
import com.yzx.web_flux_demo.net.factory.TlsContextFactory;
import com.yzx.web_flux_demo.net.handler.Http2RequestHandler;
import com.yzx.web_flux_demo.net.metrics.MetricsCollector;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @className: Http2Server
 * @author: yzx
 * @date: 2025/11/22 18:11
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class Http2Server {
    private final int port;
    private final Router router;
    private final MetricsCollector metrics;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public Http2Server(int port,  Router router, MetricsCollector metrics) {
        this.port = port;
        this.router = router;
        this.metrics = metrics;
    }

    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO)) // 可选，用于调试
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            HttpServerCodec httpServerCodec = new HttpServerCodec();
                            pipeline.addLast(httpServerCodec);
                            pipeline.addLast(new HttpObjectAggregator(65536));

                            // HTTP/2 frame codec & multiplex handler 创建
                            Http2FrameCodec http2FrameCodec = Http2FrameCodecBuilder.forServer().build();
                            Http2MultiplexHandler http2MultiplexHandler = new Http2MultiplexHandler(
                                    new ChannelInitializer<Channel>() {
                                        @Override
                                        protected void initChannel(Channel ch) {
                                            ch.pipeline().addLast(
                                                    new IdleStateHandler(0,0,30, TimeUnit.SECONDS),
                                                    new Http2RequestHandler(router, metrics /*, config 如果需要 */)
                                            );
                                        }
                                    }
                            );

                            // IMPORTANT: 把 multiplex handler 也传入 upgrade codec
                            Http2ServerUpgradeCodec http2UpgradeCodec =
                                    new Http2ServerUpgradeCodec(http2FrameCodec, http2MultiplexHandler);

                            HttpServerUpgradeHandler.UpgradeCodecFactory upgradeFactory = protocol -> {
                                if ("h2c".equals(protocol)) {
                                    return http2UpgradeCodec;
                                }
                                return null;
                            };

                            HttpServerUpgradeHandler upgradeHandler =
                                    new HttpServerUpgradeHandler(httpServerCodec, upgradeFactory, 65536);

                            pipeline.addLast("upgradeHandler", upgradeHandler);

                            // 添加一个简单的 HTTP/1.1 fallback handler（用于 Postman 或普通 HTTP 请求）
                            pipeline.addLast("http1Handler", new SimpleChannelInboundHandler<io.netty.handler.codec.http.FullHttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, io.netty.handler.codec.http.FullHttpRequest req) {
                                    // 这里可以直接使用 router 处理，或简单返回 200
                                    io.netty.handler.codec.http.DefaultFullHttpResponse resp =
                                            new io.netty.handler.codec.http.DefaultFullHttpResponse(
                                                    req.protocolVersion(),
                                                    io.netty.handler.codec.http.HttpResponseStatus.OK,
                                                    ctx.alloc().buffer().writeBytes("{\"ok\":true}".getBytes())
                                            );
                                    resp.headers().set("content-type", "application/json");
                                    resp.headers().setInt("content-length", resp.content().readableBytes());
                                    ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    ctx.close();
                                }
                            });
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("HTTP/2 Cleartext (h2c) server started on port: {}", port);
            serverChannel = future.channel();
            serverChannel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    // 停止服务器
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("HTTP/2 server stopped");
    }

    // 主函数：启动入口
    public static void main(String[] args) throws Exception {
        // 初始化配置 - 现在超级简单！
//        Http2ServerConfig config = new Http2ServerConfig();
        // 不需要设置任何SSL文件路径！
        // 初始化路由并注册示例接口
        Router router = new Router();
        // 示例1：GET /hello
        router.register("GET", "/hello", (request, callback) -> {
            String responseBody = "{\"message\": \"Hello, HTTP/2!\"}";
            callback.onSuccess(new Http2Response(
                    HttpResponseStatus.OK,
                    "application/json",
                    responseBody.getBytes()
            ));
        });

        // 示例2：GET /user/{id}
        router.register("GET", "/user/{id}", (request, callback) -> {
            String userId = request.getPathParams().get("id");
            String responseBody = "{\"userId\": \"" + userId + "\", \"name\": \"Test User\"}";
            callback.onSuccess(new Http2Response(
                    HttpResponseStatus.OK,
                    "application/json",
                    responseBody.getBytes()
            ));
        });

        // 启动服务器
        MetricsCollector metrics = new MetricsCollector();
        new Http2Server(8080, router, metrics).start();
    }
}
