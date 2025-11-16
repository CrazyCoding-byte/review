package com.yzx.crazycodingbytemq.store;

import com.yzx.crazycodingbytemq.config.ServerConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * @className: MessageStoreFactory
 * @author: yzx
 * @date: 2025/11/16 13:05
 * @Version: 1.0
 * @description:消息存储工厂
 */
@Slf4j
public class MessageStoreFactory {

    /**
     * 根据配置创建持久化策略模式实例
     * @param config 服务配置
     * @return 持久化策略
     */
    public static MessageStoreStrategy create(ServerConfig config) {
        String storeType = config.getMessageStoreType();
        switch (storeType) {
            case "file":
                return new FileMessageStore(config.getFileStoreBaseDir());
            case "rocksdb":
                log.info("使用RocksDB存储策略");
                throw new UnsupportedOperationException("RocksDB存储暂未实现");
            default:
                throw new IllegalArgumentException("不支持的消息存储类型：" + storeType);
        }
    }
}
