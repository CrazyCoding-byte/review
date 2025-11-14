package com.yzx.crazycodingbytemq.codec;

/**
 * @className: ProtocolConstant
 * @author: yzx
 * @date: 2025/11/14 15:31
 * @Version: 1.0
 * @description:
 */
public interface ProtocolConstant {
    //魔数:用于校验帧的合法性
    int MAGIC = 0xCAFEBABE;
    //协议版本
    byte Version = 0x01;
    //帧头长度：魔数(4字节) + 版本(1字节) + 消息体长度(4字节) + 消息类型(1字节)
    int FRAME_HEADER_LENGTH = 16;
}
