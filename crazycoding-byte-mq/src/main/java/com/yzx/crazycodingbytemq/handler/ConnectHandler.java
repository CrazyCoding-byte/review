package com.yzx.crazycodingbytemq.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.yzx.crazycodingbytemq.codec.ProtocolConstant;
import com.yzx.crazycodingbytemq.enums.MessageTypeEnum;
import com.yzx.crazycodingbytemq.model.MqMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import com.yzx.crazycodingbytemq.codec.ProtocolFrame;

/**
 * @className: ConnectHandler
 * @author: yzx
 * @date: 2025/11/14 19:29
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class ConnectHandler extends ChannelInboundHandlerAdapter {

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
        MqMessage.ConnectRequest request;
        try {
            request = MqMessage.ConnectRequest.parseFrom(frame.getBody());
        } catch (InvalidProtocolBufferException e) {
            log.error("解析连接请求失败", e);
            ctx.close();
            return;
        }
        log.info("受到连接请求:clientdId={},clientType={}", request.getClientId(), request.getClientType());
        //模拟连接校验(实际场景需要校验clientId合法等)
        boolean success = true;
        String message = success ? "连接成功" : "连接失败";
        //构建连接响应
        MqMessage.ConnectResponse connectResponse = new MqMessage.ConnectResponse()
                .setSuccess(success)
                .setMessage(message)
                .setServerId(request.getClientId())
                .build();
        byte[] responseBydy = connectResponse.toByteArray();
        //构建协议帧
        ProtocolFrame responseFrame = new ProtocolFrame(
                ProtocolConstant.MAGIC,
                ProtocolConstant.Version,
                responseBydy.length,
                MessageTypeEnum.CONNECT_RESPONSE.getCode(),
                responseBydy
        );
        ctx.writeAndFlush(responseFrame);
    }

}
