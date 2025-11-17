package com.yzx.crazycodingbytemq.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
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
        // 1. 校验核心协议字段（避免非法值被编码发送）
        validateFrame(frame);
        //按协议结构写入:魔数->版本->消息体长度->消息类型->消息体
        out.writeInt(frame.getMagic());
        out.writeByte(frame.getVersion());
        out.writeInt(frame.getBodyLength());
        out.writeByte(frame.getMessageType());
        out.writeBytes(frame.getBody());
    }
    /**
     * 校验ProtocolFrame的合法性，避免错误数据被发送
     */
    private void validateFrame(ProtocolFrame frame) {
        // 校验魔数是否匹配（强制使用协议常量，避免客户端传入错误值）
        if (frame.getMagic() != ProtocolConstant.MAGIC) {
            throw new EncoderException("消息魔数不合法（实际=" + frame.getMagic() + "，期望=" + ProtocolConstant.MAGIC + "）");
        }

        // 校验版本是否匹配
        if (frame.getVersion() != ProtocolConstant.Version) {
            throw new EncoderException("协议版本不支持（实际=" + frame.getVersion() + "，期望=" + ProtocolConstant.Version + "）");
        }

        // 校验消息体长度是否与实际body长度一致
        if (frame.getBodyLength() != frame.getBody().length) {
            throw new EncoderException("消息体长度与实际内容不匹配（声明长度=" + frame.getBodyLength() + "，实际长度=" + frame.getBody().length + "）");
        }

        // 校验消息体长度是否为非负数
        if (frame.getBodyLength() < 0) {
            throw new EncoderException("消息体长度不能为负数（长度=" + frame.getBodyLength() + "）");
        }
    }
}
