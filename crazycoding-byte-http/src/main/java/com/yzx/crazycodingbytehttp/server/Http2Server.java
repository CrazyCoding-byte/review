package com.yzx.crazycodingbytehttp.server;//package com.yzx.web_flux_demo.net.server;
//
//import com.yzx.web_flux_demo.net.config.*;
//import com.yzx.web_flux_demo.net.config.core.Http2Response;
//import com.yzx.web_flux_demo.net.factory.TlsContextFactory;
//import com.yzx.web_flux_demo.net.handler.Http1RequestHandler;
//import com.yzx.web_flux_demo.net.handler.Http2RequestHandler;
//import com.yzx.web_flux_demo.net.metrics.MetricsCollector;
//import io.netty.bootstrap.ServerBootstrap;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import io.netty.channel.*;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.channel.socket.nio.NioServerSocketChannel;
//import io.netty.handler.codec.http.*;
//import io.netty.handler.codec.http2.Http2FrameCodec;
//import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
//import io.netty.handler.codec.http2.Http2MultiplexHandler;
//import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
//import io.netty.handler.logging.LogLevel;
//import io.netty.handler.logging.LoggingHandler;
//import io.netty.handler.timeout.IdleStateHandler;
//import io.netty.util.ReferenceCountUtil;
//import lombok.extern.slf4j.Slf4j;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Collections;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
///**
// * @className: Http2Server
// * @author: yzx
// * @date: 2025/11/22 18:11
// * @Version: 1.0
// * @description:
// */
//@Slf4j
//public class Http2Server {
//    private final int port;
//    private final Router router;
//    private final MetricsCollector metrics;
//    private EventLoopGroup bossGroup;
//    private EventLoopGroup workerGroup;
//    private Channel serverChannel;
//
//    public Http2Server(int port, Router router, MetricsCollector metrics) {
//        this.port = port;
//        this.router = router;
//        this.metrics = metrics;
//    }
//
//    public void start() throws Exception {
//        bossGroup = new NioEventLoopGroup(1);
//        workerGroup = new NioEventLoopGroup();
//
//        try {
//            ServerBootstrap bootstrap = new ServerBootstrap()
//                    .group(bossGroup, workerGroup)
//                    .channel(NioServerSocketChannel.class)
//                    .option(ChannelOption.SO_BACKLOG, 128)
//                    .childOption(ChannelOption.SO_KEEPALIVE, true)
//                    .handler(new LoggingHandler(LogLevel.INFO)) // 可选，用于调试
//                    .childHandler(new ChannelInitializer<SocketChannel>() {
//                        @Override
//                        protected void initChannel(SocketChannel ch) {
//                            ChannelPipeline pipeline = ch.pipeline();
//                            // 1. HTTP/1.1 编解码和聚合
//                            HttpServerCodec httpServerCodec = new HttpServerCodec();
//                            pipeline.addLast(httpServerCodec); //讲字节码解析为FullHttpRequest
//                            pipeline.addLast(new HttpObjectAggregator(65536)); // 聚合请求体
//                            // 2. HTTP/2 升级配置
//                            Http2FrameCodec http2FrameCodec = Http2FrameCodecBuilder.forServer().build();
//                            Http2MultiplexHandler http2MultiplexHandler = new Http2MultiplexHandler(
//                                    new ChannelInitializer<Channel>() {
//                                        @Override
//                                        protected void initChannel(Channel ch) {
//                                            ch.pipeline().addLast(
//                                                    new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS),
//                                                    new Http2RequestHandler(router, metrics)
//                                            );
//                                        }
//                                    }
//                            );
//                            HttpServerUpgradeHandler.UpgradeCodecFactory upgradeFactory = protocol -> {
//                                // 只支持 h2c 协议升级
//                                if ("h2c".equals(protocol)) {
//                                    return new Http2ServerUpgradeCodec(http2FrameCodec, http2MultiplexHandler);
//                                }
//                                // 不支持的协议返回 null
//                                return null;
//                            };
//                            HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(
//                                    httpServerCodec,
//                                    upgradeFactory,  // 使用工厂类替代列表
//                                    65536  // maxContentLength
//                            );
//                            pipeline.addLast("upgradeHandler", upgradeHandler);
//                            // 4. HTTP/1.1 降级处理器（必须放在升级处理器之后）
//                            pipeline.addLast("http1Handler", new Http1RequestHandler(router, metrics));
//                        }
//                    });
//
//            ChannelFuture future = bootstrap.bind(port).sync();
//            log.info("HTTP/2 Cleartext (h2c) server started on port: {}", port);
//            serverChannel = future.channel();
//            serverChannel.closeFuture().sync();
//        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
//        }
//    }
//
//    // 停止服务器
//    public void stop() {
//        if (serverChannel != null) {
//            serverChannel.close();
//        }
//        if (bossGroup != null) {
//            bossGroup.shutdownGracefully();
//        }
//        if (workerGroup != null) {
//            workerGroup.shutdownGracefully();
//        }
//        log.info("HTTP/2 server stopped");
//    }
//
//    // 主函数：启动入口
//    public static void main(String[] args) throws Exception {
//        // 初始化配置 - 现在超级简单！
////        Http2ServerConfig config = new Http2ServerConfig();
//        // 不需要设置任何SSL文件路径！
//        // 初始化路由并注册示例接口
//        Router router = new Router();
//        // 示例1：GET /hello
//        router.register("GET", "/hello", (request, callback) -> {
//            String responseBody = "{\"message\": \"Hello, HTTP/2!\"}";
//            callback.onSuccess(new Http2Response(
//                    HttpResponseStatus.OK,
//                    "application/json",
//                    responseBody.getBytes()
//            ));
//        });
//        // 示例2：GET /user/{id}
//        router.register("GET", "/user/{id}", (request, callback) -> {
//            String userId = request.getPathParams().get("id");
//            String responseBody = "{\"userId\": \"" + userId + "\", \"name\": \"Test User\"}";
//            callback.onSuccess(new Http2Response(
//                    HttpResponseStatus.OK,
//                    "application/json",
//                    responseBody.getBytes()
//            ));
//        });
//
//        // 启动服务器
//        MetricsCollector metrics = new MetricsCollector();
//        new Http2Server(8080, router, metrics).start();
//    }
//}
