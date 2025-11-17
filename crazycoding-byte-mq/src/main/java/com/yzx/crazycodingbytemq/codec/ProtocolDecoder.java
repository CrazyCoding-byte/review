package com.yzx.crazycodingbytemq.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @className: ProtocolDecoder
 * @author: yzx
 * @date: 2025/11/14 13:53
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class ProtocolDecoder extends ByteToMessageDecoder {
    private final int maxFrameLength;//从配置传入最大帧长度

    public ProtocolDecoder(int maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        // 1. 半包检测：可读字节不足帧头长度，等待后续数据
        if (in.readableBytes() < ProtocolConstant.FRAME_HEADER_LENGTH) {
            return;
        }

        // 2. 标记读指针（半包时重置）
        in.markReaderIndex();

        // 3. 校验魔数（非法帧直接拒绝）
        int magic = in.readInt();
        if (magic != ProtocolConstant.MAGIC) {
            in.resetReaderIndex();
            throw new DecoderException("非法帧：魔数不匹配（实际=" + magic + "，期望=" + ProtocolConstant.MAGIC + "）");
        }

        // 4. 校验协议版本（不支持的版本直接拒绝）
        byte version = in.readByte();
        if (version != ProtocolConstant.Version) {
            in.resetReaderIndex();
            throw new DecoderException("协议版本不支持（实际=" + version + "，期望=" + ProtocolConstant.Version + "）");
        }

        // 4. 读取消息体长度和类型（严格遵循ProtocolFrame结构）
        int bodyLength = in.readInt();
        byte messageType = in.readByte();

        // 5. 校验消息体长度是否足够
        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex(); // 重置读取位置，等待完整数据
            return;
        }

        // 6. 读取消息体并构建ProtocolFrame
        byte[] body = new byte[bodyLength];
        in.readBytes(body);
        out.add(new ProtocolFrame(magic, version, bodyLength, messageType, body));
    }
}
