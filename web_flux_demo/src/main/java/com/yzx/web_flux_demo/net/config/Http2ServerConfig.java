//package com.yzx.web_flux_demo.net.config;
//
//import lombok.Data;
//import com.typesafe.config.Config;
//import com.typesafe.config.ConfigFactory;
//import java.time.Duration;
//
///**
// * @className: Http2ServerConfig
// * @author: yzx
// * @date: 2025/11/22 16:25
// * @Version: 1.0
// * @description:
// */
//@Data
//public class Http2ServerConfig {
//    // 服务器配置
//    private final int port;
//    private final int bossThreadCount;
//    private final int workerThreadCount;
//    private final Duration connectionTimeout;
//    private final Duration idleTimeout;
//
//    // TLS 配置
//    private final String keyStorePath;
//    private final String keyStorePassword;
//    private final String trustStorePath;
//    private final String trustStorePassword;
//
//    // 连接池配置
//    private final int maxConnections;
//    private final int maxPendingAcquires;
//    private final Duration acquireTimeout;
//    // SSL证书配置
//    private String sslKeyPath; // 私钥文件路径
//    private String sslCertPath; // 证书文件路径
//    private String sslKeyPassword = ""; // 私钥密码（如果有）
//
//    // 服务器配置
//    private int maxFrameSize = 16384; // HTTP/2帧最大大小
//    private int maxHeaderListSize = 8192; // 最大请求头大小
//    // 压缩配置
//    private final boolean enableCompression;
//    private final String compressionLevel;
//
//    public Http2ServerConfig() {
////        Config config = ConfigFactory.load("http2-server.conf");
////        Config serverConfig = config.getConfig("http2.server");
////        Config tlsConfig = config.getConfig("http2.tls");
////        Config poolConfig = config.getConfig("http2.connection-pool");
////        Config compressionConfig = config.getConfig("http2.compression");
////
////        // 服务器配置
////        this.port = serverConfig.getInt("port");
////        this.bossThreadCount = serverConfig.getInt("boss-thread-count");
////        this.workerThreadCount = serverConfig.getInt("worker-thread-count");
////        this.connectionTimeout = Duration.parse(serverConfig.getString("connection-timeout"));
////        this.idleTimeout = Duration.parse(serverConfig.getString("idle-timeout"));
////
////        // TLS 配置
////        this.keyStorePath = tlsConfig.getString("key-store-path");
////        this.keyStorePassword = tlsConfig.getString("key-store-password");
////        this.trustStorePath = tlsConfig.getString("trust-store-path");
////        this.trustStorePassword = tlsConfig.getString("trust-store-password");
////
////        // 连接池配置
////        this.maxConnections = poolConfig.getInt("max-connections");
////        this.maxPendingAcquires = poolConfig.getInt("max-pending-acquires");
////        this.acquireTimeout = Duration.parse(poolConfig.getString("acquire-timeout"));
////
////        // 压缩配置
////        this.enableCompression = compressionConfig.getBoolean("enable");
////        this.compressionLevel = compressionConfig.getString("level");
//
//    }
//}
