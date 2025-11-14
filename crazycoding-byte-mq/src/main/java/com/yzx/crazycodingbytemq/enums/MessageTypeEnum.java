package com.yzx.crazycodingbytemq.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @className: MessageTypeEnum
 * @author: yzx
 * @date: 2025/11/14 15:43
 * @Version: 1.0
 * @description:
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum {
    CONNECT_REQUEST((byte) 0x01, "客户端连接请求"),

    CONNECT_RESPONSE((byte) 0x02, "服务端连接响应"),

    HEARTERBEAT_RQUEST((byte) 0x03, "客户端心跳请求"),

    HEARTERBEAT_RESPONSE((byte) 0x04, "服务端心跳响应"),

    BUSSINESS_MESSAGE((byte) 0x05, "业务消息");

    private final byte code;
    private final String desc;

    public static MessageTypeEnum getByCode(byte code) {
        for (MessageTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
