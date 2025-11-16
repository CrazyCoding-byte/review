package com.yzx.crazycodingbytemq.server;

import com.yzx.crazycodingbytemq.codec.ProtocolDecoder;
import com.yzx.crazycodingbytemq.codec.ProtocolEncoder;
import com.yzx.crazycodingbytemq.config.ClientConfig;
import com.yzx.crazycodingbytemq.config.ConfigLoader;
import com.yzx.crazycodingbytemq.config.ServerConfig;
import com.yzx.crazycodingbytemq.handler.ClientResponseHandler;
import com.yzx.crazycodingbytemq.handler.HeartbeatHandler;
import com.yzx.crazycodingbytemq.model.MqMessage;
import com.yzx.crazycodingbytemq.ssl.SslContextFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelOption;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户端连接池（固定大小，复用连接）
 */
@Slf4j
@Data
public class ConnectionManager {
    //单例模式(生产级可用Dagger等依赖注入)
    private static final ConnectionManager INSTANCE = new ConnectionManager();

    public static ConnectionManager getInstance() {
        return INSTANCE;
    }

    //连接存储
    private final Map<ChannelId, ConnectionMeta> connections  = new ConcurrentHashMap<>();
    //客户端id映射
    private final Map<String, ChannelId> clientIdMapping = new ConcurrentHashMap<>();
    //当前连接数
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private final int maxConnections;

    private ConnectionManager() {
        ServerConfig config = ConfigLoader.bindConfig(ServerConfig.class, "mq.server");
        this.maxConnections = config.getMaxConnection();
    }

    /**
     * 注册新连接(带客户端id冲突)
     * @param channel
     * @param request
     * @return
     */
    public boolean register(Channel channel, MqMessage.ConnectRequest request) {
        //1.检查连接数是否超限
        if (connectionCount.get() >= maxConnections) {
            log.warn("连接数超限，当前连接数：{}", connectionCount.get());
            return false;
        }
        String clientId = request.getClientId();
        ChannelId channelId = channel.id();
        //检查客户端id是否存在
        ChannelId existingChannelId = clientIdMapping.putIfAbsent(clientId, channelId);
        if (existingChannelId != null) {
            log.warn("客户端已存在，ID: {}", clientId);
            return false;
        }
        //存储连接元数据
        ConnectionMeta meta = new ConnectionMeta();
        meta.setChannel(channel);
        meta.setClientId(clientId);
        meta.setConnectTime(Instant.now());
        meta.setLastActiveTime(Instant.now());
        meta.setClientType(request.getClientType());
        connections.put(channelId, meta);
        //连接关闭自动注销
        channel.closeFuture().addListener(future -> {
            unregister(channel);
        });
        //递增连接数
        connectionCount.incrementAndGet();
        log.info("连接注册成功:clientId={},当前连接数={}", clientId, connectionCount.get());
        return true;
    }

    /**
     *移除连接
     * @param channel
     */
    public void unregister(Channel channel) {
        if (channel == null) return;
        ChannelId id = channel.id();
        ConnectionMeta connectionMeta = connections.remove(id);
        if (connectionMeta == null){
            log.warn("注销未知连接: channelId={}", id);
            return;
        }
        connectionCount.decrementAndGet();
        log.info("连接注销:clientId={},当前连接数={}", connectionMeta.getClientId(), connectionCount.get());
        clientIdMapping.remove(connectionMeta.getClientId());
    }

    /**
     *根据clientId查询通道(后续发信息/推送)
     */
    public Channel getChannelByClientId(String clientId) {
        ChannelId channelId = clientIdMapping.get(clientId);
        if (channelId == null) return null;
        ConnectionMeta meta = connections.get(channelId);
        return meta != null ? meta.getChannel() : null;
    }

    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime(Channel channel) {
        ConnectionMeta meta = connections.get(channel.id());
        if (meta == null) return;
        meta.setLastActiveTime(Instant.now());
    }

    @Data
    public static class ConnectionMeta {
        private Channel channel;
        private String clientId;
        private String clientType;
        private Instant connectTime;
        private Instant lastActiveTime;
    }
}