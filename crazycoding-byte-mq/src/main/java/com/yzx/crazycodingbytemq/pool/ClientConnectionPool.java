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
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Timeout;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    //单例缓存:key=host:port,value=Channel
    private static final Map<String, ClientConnectionPool> POOL_CACHE = new ConcurrentHashMap<>();
    //全局循环事件组(单例,避免多连接池创建多个线程)
    private static final EventLoopGroup EVENT_LOOP_GROUP = new NioEventLoopGroup();
    //连接池核心属性
    private final ChannelPool pool;
    private final ClientConfig config;
    private final String host;
    private final int port;


    private ClientConnectionPool(String host, int port) {
        this.host = host;
        this.port = port;
        this.config = ConfigLoader.bindConfig(ClientConfig.class, "mq.client");
        this.pool = initChannelPool();
        log.info("连接池初始化完成：host={}, port={}, 池大小={}", host, port, config.getPoolSize());
    }

    /**
     * 单例获取入口（核心）
     * 同一host:port返回同一个连接池，不同地址返回不同连接池（支持多服务端）
     */
    public static ClientConnectionPool getInstance(String host, int port) {
        String key = host + ":" + port;
        // 双重检查锁：确保线程安全+高效
        if (!POOL_CACHE.containsKey(key)) {
            synchronized (ClientConnectionPool.class) {
                if (!POOL_CACHE.containsKey(key)) {
                    POOL_CACHE.put(key, new ClientConnectionPool(host, port));
                }
            }
        }
        return POOL_CACHE.get(key);
    }

    /**
     * 初始化ChannelPool(复用全局EventLoopGroup)
     */
    private ChannelPool initChannelPool() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(EVENT_LOOP_GROUP)// 使用全局EventLoopGroup
                .remoteAddress(host, port)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getConnectTimeout().toMillis());
        //SSL配置
        SslContext sslContext = null;
        if (config.isSslEnable()) {
            sslContext = SslContextFactory.createClientContext();
        }
        final SslContext finalSslContext = sslContext;
        //处理器链配置
        AbstractChannelPoolHandler poolHandler = new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel channel) throws Exception {
                var pipeline = channel.pipeline();
                if (finalSslContext != null) {
                    pipeline.addLast(finalSslContext.newHandler(channel.alloc(), host, port));
                }
                //心跳处理(30秒未消息触发)
                pipeline.addLast(new IdleStateHandler(0, config.getHeartbeatTimeout().getSeconds(), 0, TimeUnit.SECONDS));
                pipeline.addLast(new ProtocolDecoder(config.getMaxFrameLength()))
                        .addLast(new ProtocolEncoder())
                        .addLast(new HeartbeatHandler())
                        .addLast(new ClientResponseHandler());
            }

        };
        //创建固定大小的连接池
        return new FixedChannelPool(bootstrap, poolHandler, config.getPoolSize());

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
            log.debug("连接已释放回池:channelId={}", channel.id());
        } else {
            log.warn("释放连接失败，连接已关闭:channelId={}", channel != null ? channel.id() : "null");
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
        String key = host + ":" + port;
        POOL_CACHE.remove(key);
        log.info("连接池已关闭：host={}:{}", host, port);
    }
    /**
     * 全局关闭所有连接池（程序退出时调用）
     */
    public static void shutdownAll() {
        POOL_CACHE.values().forEach(ClientConnectionPool::close);
        EVENT_LOOP_GROUP.shutdownGracefully();
        log.info("所有连接池已全局关闭");
    }
}
