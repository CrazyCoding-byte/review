package com.yzx.crazycodingbytemq.config;

import lombok.Data;

import java.time.Duration;

/**
 * @className: ServerConfig
 * @author: yzx
 * @date: 2025/11/14 13:00
 * @Version: 1.0
 * @description:
 */
@Data
public class ServerConfig {
    private int port = 8080;
    private int bossThreadCount = 1;
    private int workThreadCount = Runtime.getRuntime().availableProcessors() * 2;
    private int backlog = 1024;//连接池队列大小
    private boolean keepAlive = true;
    private int sendBufSize = 65535; //发送缓冲区大小
    private int rcvBufSize = 65535;
    private Duration heartbeatTimeout = Duration.ofSeconds(30);
    private boolean sslEnable = true;
    private String sslCertPath = "cert/server.crt";
    private String sslKeyPath = "cert/server.key";
    private int maxFrameLength = 1024 * 1024 * 10;//最大消息长度
    private int maxConnection = 10000;//最大连接长度

}
