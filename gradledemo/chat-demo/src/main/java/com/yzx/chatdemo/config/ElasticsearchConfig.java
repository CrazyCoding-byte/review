package com.yzx.chatdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * @className: ElasticsearchConfig
 * @author: yzx
 * @date: 2025/12/25 23:49
 * @Version: 1.0
 * @description:
 */

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.yzx.chatdemo.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {
    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .usingSsl(buildTrustAllSslContext()) // 用信任所有证书的SSL上下文
                .withBasicAuth("elastic", "yangzhixuan")
                .withConnectTimeout(10000)
                .withSocketTimeout(30000)
                .build();
    }

    // 构建“信任所有证书”的SSL上下文（解决PKIX验证失败）
    private SSLContext buildTrustAllSslContext() {
        try {
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new java.security.SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build SSL context", e);
        }
    }
}
