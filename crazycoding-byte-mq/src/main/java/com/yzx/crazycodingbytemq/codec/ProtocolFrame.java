package com.yzx.crazycodingbytemq.codec;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @className: ProtocolFrame
 * @author: yzx
 * @date: 2025/11/14 15:33
 * @Version: 1.0
 * @description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolFrame {
    private int magic;//魔数
    private byte version;//版本号
    private int bodyLength;//消息体长度
    private byte messageType;//消息类型
    private byte[] body;//消息体
}
