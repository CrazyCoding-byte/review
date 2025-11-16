package com.yzx.crazycodingbytemq.enums;

/**
 * @className: ClientTypeEnum
 * @author: yzx
 * @date: 2025/11/16 12:52
 * @Version: 1.0
 * @description:
 */
public enum ClientTypeEnum {
    PRODUCER, CONSUMER;

    public static boolean isValid(String type) {
        try {
            ClientTypeEnum.valueOf(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // 替换校验逻辑
    private boolean isValidClientType(String clientType) {
        return ClientTypeEnum.isValid(clientType);
    }
}


