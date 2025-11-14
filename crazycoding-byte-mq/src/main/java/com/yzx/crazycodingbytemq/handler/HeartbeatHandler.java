package com.yzx.crazycodingbytemq.handler;

import com.yzx.crazycodingbytemq.codec.ProtocolConstant;
import com.yzx.crazycodingbytemq.enums.MessageTypeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import com.yzx.crazycodingbytemq.codec.ProtocolFrame;

/**
 * @className: HeartbeatHandler
 * @author: yzx
 * @date: 2025/11/14 15:48
 * @Version: 1.0
 * @description: 心跳机制入口
 */
@Slf4j
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private static final byte[] HEARTBEAT_BODY = new byte[0];

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent) {
            ProtocolFrame protocolFrame = new ProtocolFrame(
                    ProtocolConstant.MAGIC,
                    ProtocolConstant.Version,
                    HEARTBEAT_BODY.length,
                     MessageTypeEnum.HEARTERBEAT_RESPONSE.getCode(),
                    HEARTBEAT_BODY
            );
            ctx.writeAndFlush(protocolFrame);
            log.debug("send heartbeat request to server");
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ProtocolFrame frame) {
            if (frame.getMessageType() == MessageTypeEnum.HEARTERBEAT_RESPONSE.getCode()) {
                log.debug("receive heartbeat response from server");
            } else {
                ctx.fireChannelRead(msg);
            }
        }
        //非心跳消息,传给下一个处理器
        ctx.fireChannelRead(msg);
    }
}
