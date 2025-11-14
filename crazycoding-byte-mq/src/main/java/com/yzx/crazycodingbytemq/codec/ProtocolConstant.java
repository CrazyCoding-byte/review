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
    int FRAME_HEADER_LENGTH = 4 + 1 + 4 + 1; // 帧头长度（魔数4 + 版本1 + 长度4 + 类型1）
    int HEARTBEAT_TIMEOUT_SECONDS = 30; // 心跳超时时间（30秒）
}
