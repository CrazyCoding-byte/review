package com.yzx.crazycodingbytemq.config;

import lombok.Data;

import java.time.Duration;

/**
 * @className: MessageStoreConfig
 * @author: yzx
 * @date: 2025/11/16 14:02
 * @Version: 1.0
 * @description:消息存储配置（工业级全参数可配置）
 */
@Data
public class MessageStoreConfig {
    // 存储根目录
    private String baseDir = "./mq-store";
    // 单个日志文件大小上限（默认64MB）
    private long maxFileSize = 64 * 1024 * 1024;
    // 批量刷盘阈值（达到该条数触发刷盘）
    private int batchFlushThreshold = 1000;
    // 批量刷盘超时时间（默认500ms）
    private Duration batchFlushTimeout = Duration.ofMillis(500);
    // WAL日志刷盘策略（SYNC：同步刷盘，ASYNC：异步刷盘）
    private FlushPolicy flushPolicy = FlushPolicy.SYNC;
    // 日志文件保留天数（默认7天）
    private int fileRetentionDays = 7;
    // 消息校验算法（CRC32/MD5）
    private ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.CRC32;
    // 内存缓冲区大小（默认8MB）
    private int bufferSize = 8 * 1024 * 1024;
    // 崩溃恢复时的重试次数
    private int recoveryRetryCount = 3;

    // 刷盘策略枚举
    public enum FlushPolicy {
        SYNC, ASYNC
    }

    // 校验算法枚举
    public enum ChecksumAlgorithm {
        CRC32, MD5
    }
}
