package com.yzx.web_flux_demo.net.factory;

import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.Arrays;

public class TlsContextFactory {
    private static final String[] ALPN_PROTOCOLS = new String[]{"h2"}; // HTTP/2 协议

    /**
     * 创建SSL上下文 - 完全自动，无需配置
     */
    public static SslContext createSslContext() throws Exception {
        SelfSignedCertificate ssc = null;
        try {
            // 1. 自动生成自签名证书（开发测试用）
            ssc = new SelfSignedCertificate();
        } catch (CertificateException e) {
            // 在较新的JDK版本上可能不支持OpenJDK自签名证书生成器
            System.err.println("SelfSignedCertificate generation failed: " + e.getMessage());
            System.err.println("Using fallback method with specific parameters...");
            
            // 使用带参数的构造函数作为备选方案
            try {
                ssc = new SelfSignedCertificate("localhost" );
            } catch (CertificateException ce) {
                System.err.println("Fallback method also failed: " + ce.getMessage());
                System.err.println("Creating a pre-generated certificate for development...");
                
                // 最后的备选方案：抛出运行时异常，提示用户手动提供证书
                throw new RuntimeException("Unable to generate self-signed certificate automatically. " +
                        "Consider providing your own certificate files for production, " +
                        "or ensure BouncyCastle is properly configured in your environment.", e);
            }
        }

        // 2. 配置SSL构建器
        SslContextBuilder builder = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .sslProvider(SslProvider.JDK) // 使用JDK自带的SSL
                .protocols("TLSv1.2", "TLSv1.3") // 安全协议版本
                .applicationProtocolConfig(createAlpnConfig()); // HTTP/2协议协商

        return builder.build();
    }

    /**
     * 创建ALPN配置（用于协商使用HTTP/2）
     */
    private static ApplicationProtocolConfig createAlpnConfig() {
        return new ApplicationProtocolConfig(
                ApplicationProtocolConfig.Protocol.ALPN, // 使用ALPN协议
                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE, // 失败时不广告
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT, // 接受失败
                ALPN_PROTOCOLS // 只支持HTTP/2
        );
    }
}