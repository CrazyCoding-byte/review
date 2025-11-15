package com.yzx.crazycodingbytemq.handler;

import com.yzx.crazycodingbytemq.enums.MessageTypeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import com.yzx.crazycodingbytemq.codec.ProtocolFrame;

/**
 * @className: ClientResponseHandler
 * @author: yzx
 * @date: 2025/11/14 16:02
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class ClientResponseHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ProtocolFrame frame) {
            switch (MessageTypeEnum.getByCode(frame.getMessageType())) {
                case CONNECT_RESPONSE:
                    handleConnectResponse(frame);
                    return;
                case SEND_MESSAGE:
                    handleBusinessMessage(frame);
                    return;
                default:
                    log.debug("收到未知类型消息：type={}", frame.getMessageType());
            }
        }
        // 传递给后续处理器
        ctx.fireChannelRead(msg);
    }

    // 处理服务端连接响应
    private void handleConnectResponse(ProtocolFrame frame) {
        log.info("客户端收到连接响应，消息体长度：{}", frame.getBodyLength());
        // 后续可解析Protobuf结构的响应内容
    }

    // 处理业务消息
    private void handleBusinessMessage(ProtocolFrame frame) {
        log.info("客户端收到业务消息，消息体长度：{}", frame.getBodyLength());
    }
}
