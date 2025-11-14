package com.yzx.crazycodingbytemq.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @className: ProtocolEncoder
 * @author: yzx
 * @date: 2025/11/14 15:39
 * @Version: 1.0
 * @description:
 */
public class ProtocolEncoder extends MessageToByteEncoder<ProtocolFrame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolFrame frame, ByteBuf out) throws Exception {
        //按协议结构写入:魔数->版本->消息体长度->消息类型->消息体
        out.writeInt(frame.getMagic());
        out.writeByte(frame.getVersion());
        out.writeInt(frame.getBodyLength());
        out.writeByte(frame.getMessageType());
        out.writeBytes(frame.getBody());
    }
}
