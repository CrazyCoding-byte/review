package com.yzx.crazycodingbytemq.server;

import com.yzx.crazycodingbytemq.codec.ProtocolDecoder;
import com.yzx.crazycodingbytemq.codec.ProtocolEncoder;
import com.yzx.crazycodingbytemq.config.ConfigLoader;
import com.yzx.crazycodingbytemq.config.ServerConfig;
import com.yzx.crazycodingbytemq.handler.HeartbeatHandler;
import com.yzx.crazycodingbytemq.metrics.MetricHandler;
import com.yzx.crazycodingbytemq.ssl.SslContextFactory;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @className: MessageQueueServer
 * @author: yzx
 * @date: 2025/11/14 13:18
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class MessageQueueServer {
    private final ServerConfig config;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private final PrometheusMeterRegistry meterRegistry; // 指标注册表
    private Channel serverChannel;

    public MessageQueueServer() {
        this.config = ConfigLoader.bindConfig(ServerConfig.class, "mq.server");
        // 初始化线程组（带命名，便于监控）
        this.bossGroup = new NioEventLoopGroup(
                config.getBossThreadCount(),
                new DefaultThreadFactory("mq-server-boss")
        );
        this.workerGroup = new NioEventLoopGroup(
                config.getWorkerThreadCount(),
                new DefaultThreadFactory("mq-server-worker")
        );
        // 初始化指标注册表
        this.meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new JvmMetrics().bindTo(meterRegistry); // 绑定JVM指标
    }

    public void start() throws InterruptedException {
        // 1. 创建SSL上下文
        SslContext sslContext = SslContextFactory.getServerContext();

        // 2. 配置启动器
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, config.getBacklog())
                .option(ChannelOption.SO_REUSEADDR, true) // 端口复用
                .childOption(ChannelOption.SO_KEEPALIVE, config.isKeepAlive())
                .childOption(ChannelOption.TCP_NODELAY, true) // 禁用Nagle算法（低延迟）
                .childOption(ChannelOption.SO_SNDBUF, config.getSendBufSize())
                .childOption(ChannelOption.SO_RCVBUF, config.getRcvBufSize())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 3. 配置处理器链（顺序重要）
                        var pipeline = ch.pipeline();

                        // SSL处理器（最前面，先解密）
                        if (sslContext != null) {
                            SslHandler sslHandler = sslContext.newHandler(ch.alloc());
                            pipeline.addLast("ssl", sslHandler);
                        }

                        // 心跳检测
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(
                                config.getHeartbeatTimeout().getSeconds(),
                                0,
                                0,
                                TimeUnit.SECONDS
                        ));

                        // 编解码
                        pipeline.addLast("decoder", new ProtocolDecoder(config.getMaxFrameLength()));
                        pipeline.addLast("encoder", new ProtocolEncoder());
                        // 指标采集
                        pipeline.addLast("metricHandler", MetricHandler.create(meterRegistry));
                        // 业务处理器
                        pipeline.addLast("heartbeatHandler", new HeartbeatHandler());
                        pipeline.addLast("connectHandler", new );
                    }
                });

        // 4. 绑定端口
        serverChannel = bootstrap.bind(config.getPort()).sync().channel();
        log.info("消息队列服务端启动成功，端口：{}，SSL启用：{}", config.getPort(), sslContext != null);

        // 5. 注册关闭钩子（优雅关闭）
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**
     * 优雅关闭（等待正在处理的任务完成）
     */
    public void shutdown() {
        log.info("开始优雅关闭服务端...");
        try {
            // 1. 关闭服务端通道（停止接收新连接）
            if (serverChannel != null) {
                serverChannel.close().sync(5, TimeUnit.SECONDS);
            }
            // 2. 关闭事件循环组（等待现有任务完成）
            workerGroup.shutdownGracefully(10, 30, TimeUnit.SECONDS);
            bossGroup.shutdownGracefully(10, 30, TimeUnit.SECONDS);
            log.info("服务端已优雅关闭");
        } catch (Exception e) {
            log.error("服务端关闭失败", e);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new MessageQueueServer().start();
    }
}