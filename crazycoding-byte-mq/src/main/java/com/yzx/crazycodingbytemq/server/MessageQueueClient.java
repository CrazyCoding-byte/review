package com.yzx.crazycodingbytemq.server;

import com.yzx.crazycodingbytemq.codec.ProtocolConstant;
import com.yzx.crazycodingbytemq.codec.ProtocolDecoder;
import com.yzx.crazycodingbytemq.codec.ProtocolEncoder;
import com.yzx.crazycodingbytemq.config.ClientConfig;
import com.yzx.crazycodingbytemq.config.ConfigLoader;
import com.yzx.crazycodingbytemq.enums.MessageTypeEnum;
import com.yzx.crazycodingbytemq.handler.ClientResponseHandler;
import com.yzx.crazycodingbytemq.handler.HeartbeatHandler;
import com.yzx.crazycodingbytemq.model.MqMessage;
import com.yzx.crazycodingbytemq.pool.ClientConnectionPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import com.yzx.crazycodingbytemq.codec.ProtocolFrame;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


/**
 * @className: MessageQueueClient
 * @author: yzx
 * @date: 2025/11/14 18:54
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class MessageQueueClient {
    private final String host;
    private final int port;
    private final String clientId;
    private final ClientConnectionPool connectionPool;
    private final ClientConfig clientConfig;

    // 构造器：传入服务端地址+客户端ID
    public MessageQueueClient(String host, int port, String clientId) {
        this.host = host;
        this.port = port;
        this.clientId = clientId;
        this.connectionPool = ClientConnectionPool.getInstance(host, port);
        this.clientConfig = ConfigLoader.bindConfig(ClientConfig.class, "mq.client");
    }

    //启动客户端发送连接请求
    public CompletableFuture<Boolean> start() {
        CompletableFuture<Boolean> booleanCompletableFuture = new CompletableFuture<>();
        //从连接池获取连接
        connectionPool.acquire().whenComplete((channel, throwable) -> {
            if (throwable != null) {
                log.error("获取连接失败:{}", throwable.getMessage());
                booleanCompletableFuture.complete(false);
                return;
            }
            //发送连接请求
            sendConnectRequest(channel);
            booleanCompletableFuture.complete(true);
        });
        return booleanCompletableFuture;
    }

    /**
     * 发送连接请求
     */
    private void sendConnectRequest(Channel channel) {
        //发送连接请求
        MqMessage.ConnectRequest request = MqMessage.ConnectRequest.newBuilder()
                .setClientId(clientId)
                .setClientType(clientConfig.getClientType())
                .setClientVersion(clientConfig.getClientVersion())
                .build();
        byte[] body = request.toByteArray();
        ProtocolFrame protocolFrame = new ProtocolFrame(
                ProtocolConstant.MAGIC,
                ProtocolConstant.Version,
                body.length,
                MessageTypeEnum.CONNECT_REQUEST.getCode(),
                body
        );
        channel.writeAndFlush(protocolFrame);
        log.info("发送连接请求:{}", protocolFrame);
    }

    /**
     *发送消息(生产者api)
     */
    public CompletableFuture<Boolean> sendMessage(String queueName, String messageBody) {
        CompletableFuture<Boolean> booleanCompletableFuture = new CompletableFuture<>();
        long minutes = clientConfig.getConnectTimeout().toMinutes();
        connectionPool.acquire().whenComplete((channel, throwable) -> {
            if (throwable != null) {
                log.error("发送消息失败:{}", throwable.getMessage());
                booleanCompletableFuture.completeExceptionally(throwable);
                return;
            }
            //构建发送消息请求
            String str = UUID.randomUUID().toString();
            MqMessage.SendMessageRequest request = MqMessage.SendMessageRequest.newBuilder()
                    .setQueueName(queueName)
                    .setMessageId(str)
                    .setMessageBody(messageBody)
                    .setProducerClientId(clientId)
                    .setPriority(0) //默认优先级
                    .build();
            //封装协议栈
            ProtocolFrame protocolFrame = new ProtocolFrame(
                    ProtocolConstant.MAGIC,
                    ProtocolConstant.Version,
                    request.toByteArray().length,
                    MessageTypeEnum.SEND_MESSAGE.getCode(),
                    request.toByteArray()
            );
            //发送消息
            channel.writeAndFlush(protocolFrame).addListener(future -> {
                if (future.isSuccess()) {
                    log.info("发送消息成功:{}", protocolFrame);
                    booleanCompletableFuture.complete(true);
                } else {
                    log.error("发送消息失败:{}", protocolFrame);
                    booleanCompletableFuture.complete(false);
                }
            });
            //释放连接
            connectionPool.release(channel);
        });
        //超时处理
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(minutes);
                if (!booleanCompletableFuture.isDone()) {
                    booleanCompletableFuture.completeExceptionally(new RuntimeException("发送消息超时" + minutes + "分钟"));
                }
            } catch (InterruptedException e) {
                log.error("发送消息超时:{}", minutes);
                Thread.currentThread().interrupt();
                booleanCompletableFuture.completeExceptionally(e);
            }
        });
        return booleanCompletableFuture;
    }

    /**
     * 拉取消息消费者API
     * @param queueName
     * @param batchSize
     * @return
     */
    public CompletableFuture<MqMessage.PullMessageResponse> pullMessage(String queueName, int batchSize) {
        CompletableFuture<MqMessage.PullMessageResponse> future = new CompletableFuture<>();
        long seconds = clientConfig.getConnectTimeout().getSeconds();
        connectionPool.acquire().whenComplete(((channel, throwable) -> {
            if (throwable != null) {
                log.error("拉取消息失败:{}", throwable.getMessage());
                future.completeExceptionally(throwable);
                return;
            }
            //构建拉取消息请求
            MqMessage.PullMessageRequest request = MqMessage.PullMessageRequest.newBuilder()
                    .setQueueName(queueName)
                    .setConsumerClientId(clientId)
                    .setBatchSize(batchSize)
                    .build();
            //封装协议栈
            ProtocolFrame protocolFrame = new ProtocolFrame(
                    ProtocolConstant.MAGIC,
                    ProtocolConstant.Version,
                    request.toByteArray().length,
                    MessageTypeEnum.PULL_MESSAGE.getCode(),
                    request.toByteArray()
            );
            channel.writeAndFlush(protocolFrame).addListener(result -> {
                if (result.isSuccess()) {
                    log.info("拉取消息成功:queueName={},bathSize={}", protocolFrame, batchSize);
                    // 实际需在ClientResponseHandler中接收响应并回调，这里先返回空响应（后续补全）
                    future.complete(MqMessage.PullMessageResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("拉取请求已经发送")
                            .build());
                } else {
                    log.error("拉取消息请求发送失败:queueName{}", protocolFrame, result.cause());
                    future.completeExceptionally(result.cause());
                }
                connectionPool.release(channel);
            });
            //超时处理
            CompletableFuture.runAsync(() -> {
                try {
                    TimeUnit.SECONDS.sleep(seconds);
                    if (!future.isDone()) {
                        future.completeExceptionally(new RuntimeException("拉取消息超时" + seconds + "秒"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    future.completeExceptionally(e);
                }
            });
        }));
        return future;
    }

    /**
     * 关闭客户端
     */
    public void close() {
        connectionPool.close();
        log.info("客户端已关闭：clientId={}", clientId);
    }

    // 测试用main方法
    public static void main(String[] args) throws InterruptedException {
        // 初始化生产者客户端
        MessageQueueClient producer = new MessageQueueClient("127.0.0.1", 8888, "producer-001");
        // 启动客户端
        producer.start().whenComplete((success, throwable) -> {
            if (success) {
                log.info("生产者启动成功");
                // 发送消息
                producer.sendMessage("test-queue", "Hello MQ!").whenComplete((sendOk, e) -> {
                    if (sendOk) {
                        log.info("消息发送成功");
                    } else {
                        log.error("消息发送失败", e);
                    }
                });
            } else {
                log.error("生产者启动失败", throwable);
            }
        });

        // 阻塞避免程序退出
        Thread.sleep(10000);
        producer.close();
    }
}
