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
            throw new DecoderException("非法帧：魔数不匹配（实际=" + magic + "，期望=" + ProtocolConstant.MAGIC + "）");
        }

        // 4. 校验协议版本（不支持的版本直接拒绝）
        byte version = in.readByte();
        if (version != ProtocolConstant.Version) {
            throw new DecoderException("协议版本不支持（实际=" + version + "，期望=" + ProtocolConstant.Version + "）");
        }

        // 5. 校验消息体长度（防止超大消息导致OOM）
        int bodyLength = in.readInt();
        if (bodyLength < 0 || bodyLength > maxFrameLength) {
            ctx.close();
            throw new DecoderException("消息体长度非法（实际=" + bodyLength + "，最大允许=" + maxFrameLength + "）");
        }

        // 6. 读取消息类型（此处暂存，后续业务处理器用）
        byte messageType = in.readByte();

        // 7. 完整消息检测：可读字节不足消息体长度，重置读指针等待后续数据
        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }

        // 8. 读取消息体（后续交给业务处理器）
        byte[] body = new byte[bodyLength];
        in.readBytes(body);

        // 9. 封装协议帧（此处可补充ProtocolFrame类，若还缺失后续会补）
        // 注：当前先临时封装，后续补充ProtocolFrame类后替换
        ProtocolFrame frame = new ProtocolFrame(magic, version, bodyLength, messageType, body);
        out.add(frame);
    }
}
