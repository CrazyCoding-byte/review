package com.yzx.crazycodingbytemq.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.yzx.crazycodingbytemq.codec.ProtocolConstant;
import com.yzx.crazycodingbytemq.enums.MessageTypeEnum;
import com.yzx.crazycodingbytemq.model.MqMessage;
import com.yzx.crazycodingbytemq.server.ConnectionManager;
import io.micrometer.core.instrument.Counter;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import com.yzx.crazycodingbytemq.codec.ProtocolFrame;

import java.util.UUID;

/**
 * @className: ConnectHandler
 * @author: yzx
 * @date: 2025/11/14 19:29
 * @Version: 1.0
 * @description: 处理连接请求handler
 */
@Slf4j
public class ConnectHandler extends ChannelInboundHandlerAdapter {
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ProtocolFrame frame)) {
            ctx.fireChannelRead(msg);
            return;
        }
        //只处理连接请求类型
        if (frame.getMessageType() != MessageTypeEnum.CONNECT_REQUEST.getCode()) {
            ctx.fireChannelRead(msg);
            return;
        }
        //解析连接请求(Protobuf 反序列化)
        MqMessage.ConnectRequest request = null;
        try {
            request = parseAndValidateRequest(frame);
            log.info("收到连接的请求:clientId{},clientType={}", request.getClientId(), request.getClientType());
            //注册连接到请求管理中心
            boolean register = connectionManager.register(ctx.channel(), request);
            log.info("收到连接请求:clientdId={},clientType={}", request.getClientId(), request.getClientType());
            // 3. 构建响应
            String message = register ? "连接成功" : "连接失败（连接数超限或ClientId冲突）";
            MqMessage.ConnectResponse connectResponse = MqMessage.ConnectResponse.newBuilder()
                    .setSuccess(register)
                    .setMessage(message)
                    .setServerId(generateServerId()) // 修复：原代码用了clientId作为serverId，改为生成服务端ID
                    .build();
            sendResponseFrame(ctx, connectResponse, register);
        } catch (InvalidProtocolBufferException e) {
            log.error("解析连接请求失败", e);
            ctx.close();
            return;
        } catch (IllegalArgumentException e) {
            log.warn("连接请求参数无效:{}", e.getMessage());
            sendErrorResponse(ctx, e.getMessage());
        } catch (Exception e) {
            log.error("处理连接请求时发生未知错误", e);
            handleUnknowError(ctx);
        }
    }

    //连接断开时触发
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        connectionManager.unregister(ctx.channel());
    }

    //通道异常时关闭
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("连接异常", cause);
        connectionManager.unregister(ctx.channel());
        ctx.close(); // 异常连接直接关闭
    }

    private void sendResponseFrame(ChannelHandlerContext ctx, MqMessage.ConnectResponse response, boolean isSuccess) {
        byte[] responseBytes = response.toByteArray();
        ProtocolFrame responseFrame = new ProtocolFrame(
                ProtocolConstant.MAGIC,
                ProtocolConstant.Version,
                responseBytes.length,
                MessageTypeEnum.CONNECT_RESPONSE.getCode(),
                responseBytes
        );
        // 失败响应发送后关闭连接
        if (!isSuccess) {
            ctx.writeAndFlush(responseFrame).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        ctx.writeAndFlush(responseFrame);
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, String errorMsg) {
        MqMessage.ConnectResponse response = MqMessage.ConnectResponse.newBuilder()
                .setSuccess(false)
                .setMessage("连接失败:" + errorMsg)
                .setServerId(generateServerId())
                .build();
        sendResponseFrame(ctx, response, false);
    }

    //解析并校验连接请求
    private MqMessage.ConnectRequest parseAndValidateRequest(ProtocolFrame frame) throws InvalidProtocolBufferException {
        MqMessage.ConnectRequest request = MqMessage.ConnectRequest.parseFrom(frame.getBody());
        //严格校验核心参数

        // 严格校验核心参数
        if (request.getClientId() == null || request.getClientId().trim().isEmpty()) {
            throw new IllegalArgumentException("ClientId不能为空");
        }
        if (request.getClientType() == null || !isValidClientType(request.getClientType())) {
            throw new IllegalArgumentException("无效的ClientType：" + request.getClientType());
        }
        if (request.getClientVersion() == null || !isValidVersion(request.getClientVersion())) {
            throw new IllegalArgumentException("无效的客户端版本：" + request.getClientVersion());
        }

        return request;
    }

    private void handleUnknowError(ChannelHandlerContext ctx) {
        MqMessage.ConnectResponse response = MqMessage.ConnectResponse.newBuilder()
                .setSuccess(false)
                .setMessage("未知错误")
                .setServerId(generateServerId())
                .build();
        ProtocolFrame protocolFrame = new ProtocolFrame(
                ProtocolConstant.MAGIC,
                ProtocolConstant.Version,
                response.getSerializedSize(),
                MessageTypeEnum.CONNECT_RESPONSE.getCode(),
                response.toByteArray()
        );
        // 发送后关闭连接+注销
        ctx.writeAndFlush(protocolFrame).addListener(future -> {
            connectionManager.unregister(ctx.channel());
            ctx.close();
        });
    }
    // ------------------------------ 辅助校验方法 ------------------------------

    /**
     * 校验客户端类型是否合法（如PRODUCER/CONSUMER）
     */
    private boolean isValidClientType(String clientType) {
        return "PRODUCER".equals(clientType) || "CONSUMER".equals(clientType);
    }

    /**
     * 校验版本格式（简单示例：x.y.z）
     */
    private boolean isValidVersion(String version) {
        return version.matches("^\\d+\\.\\d+\\.\\d+$");
    }

    /**
     * 生成服务端ID（实际场景可能从配置中心获取）
     */
    private String generateServerId() {
        // 简化实现，实际可能是"server-192.168.1.100:8888"
        return "server-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
