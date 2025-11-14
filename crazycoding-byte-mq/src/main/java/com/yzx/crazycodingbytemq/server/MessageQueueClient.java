package com.yzx.crazycodingbytemq.server;

import com.yzx.crazycodingbytemq.codec.ProtocolConstant;
import com.yzx.crazycodingbytemq.codec.ProtocolDecoder;
import com.yzx.crazycodingbytemq.codec.ProtocolEncoder;
import com.yzx.crazycodingbytemq.enums.MessageTypeEnum;
import com.yzx.crazycodingbytemq.handler.ClientResponseHandler;
import com.yzx.crazycodingbytemq.handler.HeartbeatHandler;
import com.yzx.crazycodingbytemq.model.MqMessage;
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
    private Channel channel;

    public MessageQueueClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        //拿到channelHandler执行器链
                        ChannelPipeline pipeline = channel.pipeline();
                        //客户端:30s没发消息则发送心跳
                        pipeline.addLast(new IdleStateHandler(0, // 不检测读超时
                                        ProtocolConstant.HEARTBEAT_TIMEOUT_SECONDS,
                                        0,
                                        TimeUnit.SECONDS))
                                .addLast(new ProtocolDecoder())
                                .addLast(new ProtocolEncoder())
                                .addLast(new HeartbeatHandler())
                                .addLast(new ClientResponseHandler());
                    }
                });
        //连接服务器
        channel = bootstrap.connect(host, port).sync().channel();
        log.info("客户端启动成功:{}:{}", host, port);
        //发送连接请求
        sendConnectRequest();
    }

    private void sendConnectRequest() {
        //发送连接请求
        MqMessage.ConnectRequest request = MqMessage.ConnectRequest.newBuilder()
                .setClientId("client-1")
                .setClientType("PRODUCER")
                .setClientVersion("1.0.0")
                .build();
        byte[] body = request.toByteArray();
        int length = body.length;
        ProtocolFrame protocolFrame = new ProtocolFrame(
                ProtocolConstant.MAGIC,
                ProtocolConstant.Version,
                length,
                MessageTypeEnum.CONNECT_REQUEST.getCode()
                body
        );
        channel.writeAndFlush(protocolFrame);
        log.info("发送连接请求:{}", protocolFrame);
    }

    public static void main(String[] args) throws InterruptedException {
        new MessageQueueClient("127.0.0.1", 8888).start(); // 连接本地服务端
    }
}
