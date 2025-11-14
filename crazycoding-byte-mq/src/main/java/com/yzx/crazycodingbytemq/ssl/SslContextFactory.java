package com.yzx.crazycodingbytemq.ssl;

import com.yzx.crazycodingbytemq.config.ClientConfig;
import com.yzx.crazycodingbytemq.config.ConfigLoader;
import com.yzx.crazycodingbytemq.config.ServerConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.io.File;

/**
 * @className: SslContextFactory
 * @author: yzx
 * @date: 2025/11/14 13:07
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class SslContextFactory {
    @SneakyThrows(SSLException.class)
    public static SslContext getServerContext() {
        ServerConfig config = ConfigLoader.bindConfig(ServerConfig.class, "mq.server");
        if (!config.isSslEnable()) {
            log.warn("ssl服务未启动");
            return null;
        }
        File cert = new File(config.getSslCertPath());
        File key = new File(config.getSslKeyPath());
        validateFile(cert, "ssl证书");
        validateFile(key, "ssl私钥");
        //构建ssl上下文
        return SslContextBuilder.forServer(cert, key)
                .sslProvider(SslProvider.JDK)
                .clientAuth(ClientAuth.REQUIRE)
                .build();
    }

    @SneakyThrows(SSLException.class)
    public static SslContext createClientContext() {
        ClientConfig config = ConfigLoader.bindConfig(ClientConfig.class, "mq.client");
        if (!config.isSslEnable()) {
            log.warn("ssl客户端未启动");
            return null;
        }
        File cert = new File(config.getSslTruestCertPath());
        validateFile(cert, "ssl信任证书");
        return SslContextBuilder.forClient()
                .sslProvider(SslProvider.JDK)
                .trustManager(cert) //信任的CA证书
                .build();
    }

    private static void validateFile(File file, String desc) {
        if (!file.exists()) {
            log.error("{}文件不存在", desc);
            throw new RuntimeException("文件不存在");
        }
        if (!file.canRead()) {
            log.error("{}文件不可读", desc);
            throw new RuntimeException("文件不可读");
        }
    }
}
