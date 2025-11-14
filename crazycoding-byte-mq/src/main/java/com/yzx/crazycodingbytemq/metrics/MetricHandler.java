package com.yzx.crazycodingbytemq.metrics;

import com.yzx.crazycodingbytemq.server.ConnectionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.handler.codec.DecoderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;

/**
 * 指标采集处理器（记录消息吞吐量、延迟等）
 */
@Slf4j
@RequiredArgsConstructor
public class MetricHandler extends ChannelInboundHandlerAdapter {
    private final MeterRegistry meterRegistry;
    // 消息接收计数器
    private final Counter messageReceivedCounter;
    private final Counter messageErrorCounter; // 新增：消息处理错误计数
    private final Counter connectionFailedCounter; // 新增：连接失败计数
    // 消息处理计时器
    private final Timer messageProcessTimer;

    // 静态方法：创建指标处理器
    public static MetricHandler create(MeterRegistry registry) {
        return new MetricHandler(
                registry,
                Counter.builder("mq.messages.received").register(registry),
                Counter.builder("mq.messages.errors").register(registry), // 错误计数
                Counter.builder("mq.connections.failed").register(registry), // 连接失败计数
                Timer.builder("mq.messages.processed").register(registry)
        );
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        messageReceivedCounter.increment();
        long start = System.nanoTime();
        try {
            ctx.fireChannelRead(msg);
        } catch (Exception e) {
            messageErrorCounter.increment(); // 记录处理错误
            log.error("消息处理异常", e);
            throw e; // 继续向上传递，由上层处理
        } finally {
            messageProcessTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 记录活跃连接数
        meterRegistry.gauge("mq.communication.connections.active",
                ConnectionManager.getInstance().getConnectionCount());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 更新活跃连接数
        meterRegistry.gauge("mq.communication.connections.active",
                ConnectionManager.getInstance().getConnectionCount());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 记录连接级错误（如SSL握手失败、解码失败）
        if (cause instanceof SSLException) {
            meterRegistry.counter("mq.ssl.errors").increment();
        } else if (cause instanceof DecoderException) {
            meterRegistry.counter("mq.decode.errors").increment();
        }
        log.error("连接异常", cause);
        ctx.close(); // 异常连接直接关闭
    }
}