package com.yzx.crazycodingbytemq.pool;

import com.yzx.crazycodingbytemq.codec.ProtocolDecoder;
import com.yzx.crazycodingbytemq.codec.ProtocolEncoder;
import com.yzx.crazycodingbytemq.config.ClientConfig;
import com.yzx.crazycodingbytemq.config.ConfigLoader;
import com.yzx.crazycodingbytemq.handler.ClientResponseHandler;
import com.yzx.crazycodingbytemq.handler.HeartbeatHandler;
import com.yzx.crazycodingbytemq.ssl.SslContextFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Timeout;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @className: ClientConnectionPool
 * @author: yzx
 * @date: 2025/11/14 17:54
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class ClientConnectionPool {
    private final ChannelPool pool;
    private final ClientConfig config;
    private final String host;
    private final int port;

    public ClientConnectionPool(String host, int port) {
        this.config = ConfigLoader.bindConfig(ClientConfig.class, "mq.client");
        this.host = host;
        this.port = port;
        //初始化bootstrap
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.remoteAddress(host, port)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getConnectTimeout().toMinutes());
        //2.配置Channel初始化器(与服务器对应)
        SslContext sslContext = SslContextFactory.createClientContext();
        AbstractChannelPoolHandler abstractChannelPoolHandler = new AbstractChannelPoolHandler() {

            @Override
            public void channelCreated(Channel channel) throws Exception {
                // 初始化ChannelPipeline（与服务端一致的处理器链）
                channel.pipeline()
                        .addLast(sslContext.newHandler(channel.alloc(), host, port)) // SSL处理器
                        .addLast(new IdleStateHandler(0, config.getHeartbeatTimeout().getSeconds(), 0, TimeUnit.SECONDS))
                        .addLast(new ProtocolDecoder())
                        .addLast(new ProtocolEncoder())
                        .addLast(new HeartbeatHandler())
                        .addLast(new ClientResponseHandler());
            }
        };
        //3.创建固定大小连接池
        this.pool = new FixedChannelPool(bootstrap, abstractChannelPoolHandler, config.getPoolSize());
        log.info("客户端连接池初始化完成:host={},port={},池大小", host, port, config.getPoolSize());
    }

    /**
     * 从池获取连接("带重试")
     */
    public CompletableFuture<Channel> acquire() {
        CompletableFuture<Channel> channelCompletableFuture = new CompletableFuture<>();
        acquireWithRetry(channelCompletableFuture, config.getRetryCount());
        return channelCompletableFuture;
    }

    private void acquireWithRetry(CompletableFuture<Channel> channelCompletableFuture, int remainingRetries) {
        pool.acquire().addListener(future -> {
            if (future.isSuccess()) {
                Channel channel = (Channel) future.getNow();
                if (channel.isActive()) {
                    channelCompletableFuture.complete(channel);
                } else {
                    log.warn("连接已关闭，正在重试...剩余重试次数:{}", remainingRetries);
                    pool.release(channel);
                    retryAcquire(channelCompletableFuture, remainingRetries);
                }
            } else {
                //获取失败,重试
                retryAcquire(channelCompletableFuture, remainingRetries);
            }
        });
    }

    private void retryAcquire(CompletableFuture<Channel> completableFuture, int remainRetires) {
        if (remainRetires <= 0) {
            completableFuture.completeExceptionally(new Exception("获取连接失败，已达最大重试次数"));
            return;
        }
        try {
            TimeUnit.MICROSECONDS.sleep(config.getRetryInterval().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            completableFuture.completeExceptionally(e);
            return;
        }
        acquireWithRetry(completableFuture, remainRetires - 1);
    }

    /**
     * 释放连接回池
     */
    public void release(Channel channel) {
        if (channel != null && channel.isActive()) {
            pool.release(channel);
        } else {
            log.warn("释放连接失败，连接已关闭");
            if (channel != null) {
                channel.close();
            }
        }
    }

    /**
     * 关闭连接池
     */
    public void close() {
        pool.close();
        log.info("客户端连接池已关闭");
    }
}
