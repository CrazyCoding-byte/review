package com.yzx.crazycodingbytemq.server;

import com.yzx.crazycodingbytemq.codec.ProtocolDecoder;
import com.yzx.crazycodingbytemq.codec.ProtocolEncoder;
import com.yzx.crazycodingbytemq.config.ConfigLoader;
import com.yzx.crazycodingbytemq.config.ServerConfig;
import com.yzx.crazycodingbytemq.handler.ConnectHandler;
import com.yzx.crazycodingbytemq.handler.HeartbeatHandler;
import com.yzx.crazycodingbytemq.metrics.MetricHandler;
import com.yzx.crazycodingbytemq.ssl.SslContextFactory;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
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

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
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
    private final PrometheusMeterRegistry meterRegistry;
    private Channel serverChannel;

    public MessageQueueServer() {
        this.config = ConfigLoader.bindConfig(ServerConfig.class, "mq.server");
        // 修复：方法名拼写错误（Work → Worker）
        this.bossGroup = new NioEventLoopGroup(
                config.getBossThreadCount(),
                new DefaultThreadFactory("mq-server-boss")
        );
        this.workerGroup = new NioEventLoopGroup(
                config.getWorkThreadCount(), // 修复：正确方法名
                new DefaultThreadFactory("mq-server-worker")
        );
        this.meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
    }

    public void start() throws InterruptedException, IOException, UnrecoverableEntryException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        SslContext sslContext = SslContextFactory.createServerSslContext();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, config.getBacklog())
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, config.isKeepAlive())
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, config.getSendBufSize())
                .childOption(ChannelOption.SO_RCVBUF, config.getRcvBufSize())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        var pipeline = ch.pipeline();

                        if (sslContext != null) {
                            SslHandler sslHandler = sslContext.newHandler(ch.alloc());
                            pipeline.addLast("ssl", sslHandler);
                        }

                        pipeline.addLast("idleStateHandler", new IdleStateHandler(
                                config.getHeartbeatTimeout().getSeconds(),
                                0,
                                0,
                                TimeUnit.SECONDS
                        ));

                        pipeline.addLast("decoder", new ProtocolDecoder(config.getMaxFrameLength()));
                        pipeline.addLast("encoder", new ProtocolEncoder());
                        pipeline.addLast("metricHandler", MetricHandler.create(meterRegistry));
                        pipeline.addLast("heartbeatHandler", new HeartbeatHandler());
                        // 修复：补全ConnectHandler实例化
                        pipeline.addLast("connectHandler", new ConnectHandler());
                    }
                });

        serverChannel = bootstrap.bind(config.getPort()).sync().channel();
        log.info("消息队列服务端启动成功，端口：{}，SSL启用：{}", config.getPort(), sslContext != null);

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public void shutdown() {
        log.info("开始优雅关闭服务端...");
        try {
            if (serverChannel != null && serverChannel.isOpen()) {
                boolean closed = serverChannel.close().await(5, TimeUnit.SECONDS);
                if (!closed) {
                    log.warn("服务端通道关闭超时，强制关闭");
                }
            }

            if (!workerGroup.isShuttingDown()) {
                workerGroup.shutdownGracefully(10, 30, TimeUnit.SECONDS).sync();
            }

            if (!bossGroup.isShuttingDown()) {
                bossGroup.shutdownGracefully(10, 30, TimeUnit.SECONDS).sync();
            }

            log.info("服务端已优雅关闭");
        } catch (Exception e) {
            log.error("服务端关闭异常，强制终止", e);
            workerGroup.shutdownNow();
            bossGroup.shutdownNow();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException, UnrecoverableEntryException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        new MessageQueueServer().start();
    }
}