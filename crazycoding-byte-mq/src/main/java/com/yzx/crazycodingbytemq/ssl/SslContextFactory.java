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
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * @className: SslContextFactory
 * @author: yzx
 * @date: 2025/11/14 13:07
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class SslContextFactory {

    /**
     * 创建服务端SSL上下文（支持普通证书/带密码的PKCS#12格式证书）
     */
    public static SslContext createServerSslContext() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableEntryException {
        ServerConfig config = ConfigLoader.bindConfig(ServerConfig.class, "mq.server");
        if (!config.isSslEnable()) {
            log.warn("服务端SSL未启用，通信不加密（生产环境不建议）");
            return null;
        }
        // 1. 修复：实现缺失的文件校验方法
        File certFile = validateAndGetFile(config.getSslCertPath(), "SSL证书");
        File keyFile = null;
        // PKCS#12格式（.p12/.pfx）的证书是"证书+私钥"的密钥库，不需要单独的keyFile
        if (!isPkcs12Format(config.getSslCertPath())) {
            keyFile = validateAndGetFile(config.getSslKeyPath(), "SSL私钥");
        }
        // 2. 修复：证书密码处理（兼容空密码）
        String keyPassword = config.getSslKeyPassword();
        keyPassword = (keyPassword == null || keyPassword.trim().isEmpty()) ? null : keyPassword.trim();
        SslContextBuilder builder;
        // 3. 修复：方法重载不匹配（区分普通证书和PKCS#12证书）
        if (isPkcs12Format(config.getSslCertPath())) {
            // 1. 加载PKCS#12格式的密钥库
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream certInputStream = new FileInputStream(certFile)) {
                keyStore.load(certInputStream, keyPassword.toCharArray());
            }
            // 2. 提取密钥库中的私钥和证书链（默认取第一个条目）
            String alias = keyStore.aliases().nextElement();
            KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias,
                    new KeyStore.PasswordProtection(keyPassword.toCharArray()));
            PrivateKey privateKey = keyEntry.getPrivateKey();
            X509Certificate[] certChain = (X509Certificate[]) keyEntry.getCertificateChain();

            // 3. 用私钥+证书链初始化SslContextBuilder
            builder = SslContextBuilder.forServer(privateKey, certChain);
        } else {
            // 普通证书：证书文件+私钥文件+可选密码
            builder = SslContextBuilder.forServer(certFile, keyFile, keyPassword);
        }

        // 4. 修复：客户端认证策略配置
        builder.clientAuth(config.isSslClientAuthRequired() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL);

        // 使用JDK提供的SSL实现（兼容性更好）
        return builder.sslProvider(SslProvider.JDK).build();
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

    /**
     * 校验文件是否存在、可读
     */
    private static File validateAndGetFile(String filePath, String desc) throws IOException {
        Objects.requireNonNull(filePath, desc + "路径不能为空");
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException(desc + "文件不存在：" + filePath);
        }
        if (!file.canRead()) {
            throw new IOException(desc + "文件不可读：" + filePath);
        }
        return file;
    }

    /**
     * 判断是否是PKCS#12格式的证书（.p12/.pfx）
     */
    private static boolean isPkcs12Format(String certPath) {
        if (certPath == null) return false;
        String lowerPath = certPath.toLowerCase();
        return lowerPath.endsWith(".p12") || lowerPath.endsWith(".pfx");
    }
}
