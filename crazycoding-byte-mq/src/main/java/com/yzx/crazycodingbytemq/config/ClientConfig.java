package com.yzx.crazycodingbytemq.config;

import lombok.Data;

import java.time.Duration;

/**
 * @className: ClientConfig
 * @author: yzx
 * @date: 2025/11/14 13:22
 * @Version: 1.0
 * @description:
 */
@Data
public class ClientConfig {
    // 客户端标识（必须，服务端用于区分不同客户端）
    private String clientId;
    // 客户端类型（PRODUCER/CONSUMER，服务端用于区分角色）
    private String clientType;
    // 客户端版本（用于兼容性校验）
    private String clientVersion;
    private Duration connectTimeout = Duration.ofSeconds(5); // 连接超时时间
    private int retryCount = 3; // 重试次数
    private Duration retryInterval = Duration.ofSeconds(1); // 重试间隔时间
    private int poolSize = 8; // 连接池大小
    private boolean sslEnable = true;
    private String sslTrustCertPath = "conf/ca.crt"; // 修复拼写：Truest→Trust
    private String sslTrustPassword = ""; // 信任库密码
    private Duration heartbeatTimeout = Duration.ofSeconds(30); // 心跳超时时间
    private int maxFrameLength = 1024 * 1024 * 10; // 最大帧长度（10MB）
}

