package com.yzx.crazycodingbytemq.server;

import com.yzx.crazycodingbytemq.codec.ProtocolConstant;
import com.yzx.crazycodingbytemq.enums.MessageTypeEnum;
import com.yzx.crazycodingbytemq.model.MqMessage;
import com.yzx.crazycodingbytemq.pool.ClientConnectionPool;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.yzx.crazycodingbytemq.codec.ProtocolFrame;

/**
 * @className: MessageQueueConsumer
 * @author: yzx
 * @date: 2025/11/15 19:46
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class MessageQueueConsumer {
    private final String clientId;
    private final ClientConnectionPool connectionPool;
    private final long timeout = 3000;
    private final String host;
    private final int port;

    // 构造器：需要传入服务端地址+端口+客户端ID（和生产者保持一致的初始化方式）
    public MessageQueueConsumer(String clientId, int port, String host) {
        this.host = host;
        this.port = port;
        this.clientId = clientId;
        // 获取单例连接池（同一host:port全局唯一）
        this.connectionPool = ClientConnectionPool.getInstance(host, port);
        connect();
    }

    public void connect() {
        connectionPool.acquire().whenComplete((channel, throwable) -> {
            if (throwable != null) {
                log.error("消费者连接失败：clientId={}", clientId, throwable);
                throw new RuntimeException("连接服务端失败", throwable);
            }
            log.info("消费者连接成功：clientId={}, channelId={}", clientId, channel.id());
            connectionPool.release(channel);
        });
    }

    /**
     * 拉取消息
     */
    public CompletableFuture<List<MqMessage.MessageItem>> pullMessage(String queueName, int batchSize) {
        CompletableFuture<List<MqMessage.MessageItem>> future = new CompletableFuture<>();

        // 1. 从连接池获取连接（异步获取，带重试）
        connectionPool.acquire().whenComplete((channel, throwable) -> {
            if (throwable != null) {
                log.error("拉取消息失败：获取连接异常", throwable);
                future.completeExceptionally(throwable);
                return;
            }

            if (channel == null || !channel.isActive()) {
                future.completeExceptionally(new RuntimeException("无可用连接"));
                return;
            }

            // 2. 构建拉取消息请求
            MqMessage.PullMessageRequest request = MqMessage.PullMessageRequest.newBuilder()
                    .setQueueName(queueName)
                    .setConsumerClientId(clientId)
                    .setBatchSize(batchSize)
                    .build();

            // 3. 封装协议帧
            ProtocolFrame frame = new ProtocolFrame(
                    ProtocolConstant.MAGIC,
                    ProtocolConstant.Version,
                    request.toByteArray().length,
                    MessageTypeEnum.PULL_MESSAGE.getCode(),
                    request.toByteArray()
            );

            // 4. 发送请求并监听响应
            ChannelFuture channelFuture = channel.writeAndFlush(frame);
            channelFuture.addListener(result -> {
                if (result.isSuccess()) {
                    log.info("拉取消息请求发送成功：queueName={}, batchSize={}", queueName, batchSize);
                    // 注意：实际需在ClientResponseHandler中接收响应并回调
                    // 这里临时返回空列表，后续需完善响应处理逻辑
                    future.complete(List.of());
                } else {
                    log.error("拉取消息请求发送失败：queueName={}, batchSize={}", queueName, batchSize, result.cause());
                    future.completeExceptionally(result.cause());
                }
                // 释放连接回池
                connectionPool.release(channel);
            });
        });
        // 5. 超时处理
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(timeout);
                if (!future.isDone()) {
                    future.completeExceptionally(new RuntimeException("拉取消息超时（" + timeout + "ms）"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * 消息消费确认
     */
    public CompletableFuture<Boolean> ackMessage(String queueName, String messageId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // 从连接池获取连接
        connectionPool.acquire().whenComplete((channel, throwable) -> {
            if (throwable != null) {
                log.error("消息确认失败：获取连接异常", throwable);
                future.completeExceptionally(throwable);
                return;
            }

            if (channel == null || !channel.isActive()) {
                future.completeExceptionally(new RuntimeException("无可用连接"));
                return;
            }

            // 构建确认请求
            MqMessage.MessageAckRequest request = MqMessage.MessageAckRequest.newBuilder()
                    .setQueueName(queueName)
                    .setConsumerClientId(clientId)
                    .setMessageId(messageId)
                    .build();

            // 封装协议帧
            ProtocolFrame frame = new ProtocolFrame(
                    ProtocolConstant.MAGIC,
                    ProtocolConstant.Version,
                    request.toByteArray().length,
                    MessageTypeEnum.MESSAGE_ACK.getCode(),
                    request.toByteArray()
            );

            // 发送确认请求
            ChannelFuture channelFuture = channel.writeAndFlush(frame);
            channelFuture.addListener(result -> {
                if (result.isSuccess()) {
                    log.info("消息确认请求发送成功：messageId={}", messageId);
                    future.complete(true);
                } else {
                    log.error("消息确认请求发送失败：messageId={}", messageId, result.cause());
                    future.complete(false);
                }
                // 释放连接
                connectionPool.release(channel);
            });
        });

        return future;
    }

    /**
     * 关闭消费者
     */
    public void close() {
        connectionPool.close();
        log.info("消费者关闭：clientId={}", clientId);
    }

    // 测试方法
    public static void main(String[] args) throws InterruptedException {
        // 连接本地服务端，客户端ID为consumer-001
        MessageQueueConsumer consumer = new MessageQueueConsumer("127.0.0.1", 8888, "consumer-001");
        // 拉取消息
        consumer.pullMessage("test-queue", 10).whenComplete((messages, e) -> {
            if (e != null) {
                log.error("拉取消息失败", e);
                return;
            }
            log.info("拉取到{}条消息", messages.size());
            // 消费后确认（示例）
            for (MqMessage.MessageItem msg : messages) {
                consumer.ackMessage("test-queue", msg.getMessageId());
            }
        });
        // 阻塞避免退出
        Thread.sleep(10000);
        consumer.close();
    }

}