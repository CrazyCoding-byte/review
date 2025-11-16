package com.yzx.crazycodingbytemq.store;

import lombok.Getter;

/**
 * @className: MessageStoreManager
 * @author: yzx
 * @date: 2025/11/16 13:57
 * @Version: 1.0
 * @description: 消息存储管理器
 */
public class MessageStoreManager {
    @Getter
    private static final MessageStoreManager INSTANCE = new MessageStoreManager();

}
