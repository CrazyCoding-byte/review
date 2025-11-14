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
    private Duration connectTimeout = Duration.ofSeconds(5); //连接超时时间
    private int retryCount = 3; //重试次数
    private Duration retryInterval = Duration.ofSeconds(1); //重试间隔时间
    private int poolSize = 8; //连接池大小
    private boolean sslEnable = true;
    private String sslTruestCertPath = "conf/ca.crt"; //信任CA证书路径
    private String sslTruestPassword = ""; //信任库密码
    private Duration heartbeatTimeout = Duration.ofSeconds(30); //心跳超时时间
    private int maxFrameLength = 1024 * 1024 * 10;
}

