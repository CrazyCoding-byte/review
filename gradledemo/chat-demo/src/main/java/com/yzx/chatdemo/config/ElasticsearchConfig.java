package com.yzx.chatdemo.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
public class ElasticsearchConfig {


    // 统一构建ES客户端（只保留一个，避免冲突）
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            // 1. 构建信任所有证书的SSL上下文（解决HTTPS证书验证问题）
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((X509Certificate[] chain, String authType) -> true)
                    .build();

            // 2. 配置认证（二选一：BasicAuth 或 ApiKey，这里用BasicAuth示例）
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials("elastic", "yangzhixuan") // 你的ES账号密码
            );

            // 3. 构建RestClient
            RestClientBuilder builder = RestClient.builder(HttpHost.create("https://localhost:9200"))
                    .setHttpClientConfigCallback(httpClientBuilder ->
                            httpClientBuilder
                                    .setSSLContext(sslContext)
                                    .setDefaultCredentialsProvider(credentialsProvider)
                    );

            RestClient restClient = builder.build();

            // 4. 构建ElasticsearchClient
            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper()));
            return new ElasticsearchClient(transport);

        } catch (Exception e) {
            throw new RuntimeException("ES客户端初始化失败", e);
        }
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // 如果需要，可以配置日期格式
        // objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}