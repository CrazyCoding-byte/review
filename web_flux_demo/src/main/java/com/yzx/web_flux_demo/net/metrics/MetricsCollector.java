package com.yzx.web_flux_demo.net.metrics;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
/**
 * @className: MetricsCollector
 * @author: yzx
 * @date: 2025/11/22 16:43
 * @Version: 1.0
 * @description:
 */
public class MetricsCollector {
    // 请求总数计数器
    private static final Counter REQUEST_COUNT = Counter.build()
            .name("http2_server_requests_total")
            .help("Total number of HTTP/2 requests")
            .labelNames("method", "path", "status")
            .register();

    // 错误计数器
    private static final Counter ERROR_COUNT = Counter.build()
            .name("http2_server_errors_total")
            .help("Total number of HTTP/2 errors")
            .labelNames("error_type")
            .register();

    // 请求延迟直方图（单位：毫秒）
    private static final Histogram REQUEST_DURATION = Histogram.build()
            .name("http2_server_request_duration_ms")
            .help("Duration of HTTP/2 requests in milliseconds")
            .labelNames("method", "path")
            .buckets(10, 50, 100, 200, 500, 1000) // 延迟分桶
            .register();

    // 记录请求数
    public void incrementRequestCount(String method, String path, String status) {
        REQUEST_COUNT.labels(method, path, status).inc();
    }

    // 记录错误数
    public void incrementErrorCount(String errorType) {
        ERROR_COUNT.labels(errorType).inc();
    }

    // 记录请求延迟
    public void recordRequestDuration(String method, String path, long durationMs) {
        REQUEST_DURATION.labels(method, path).observe(durationMs);
    }

    // 简化方法（默认标签）
    public void incrementRequestCount() {
        incrementRequestCount("unknown", "unknown", "unknown");
    }

    public void incrementErrorCount() {
        incrementErrorCount("unknown");
    }

    public void recordRequestDuration(long durationMs) {
        recordRequestDuration("unknown", "unknown", durationMs);
    }
}
