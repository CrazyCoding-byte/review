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
    // 消息处理计时器
    private final Timer messageProcessTimer;

    // 静态方法：创建指标处理器
    public static MetricHandler create(MeterRegistry registry) {
        Counter receivedCounter = Counter.builder("mq.communication.messages.received")
                .description("收到的消息总数")
                .register(registry);
        Timer processTimer = Timer.builder("mq.communication.messages.processed")
                .description("消息处理耗时")
                .register(registry);
        return new MetricHandler(registry, receivedCounter, processTimer);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 1. 计数+1
        messageReceivedCounter.increment();
        // 2. 记录处理耗时
        long start = System.nanoTime();
        try {
            ctx.fireChannelRead(msg); // 传递给下一个处理器
        } finally {
            long duration = System.nanoTime() - start;
            messageProcessTimer.record(duration, TimeUnit.NANOSECONDS);
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
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //记录连接数错误(ssl握手失败、解码失败)
        if (cause instanceof SSLException) {
            meterRegistry.counter("mq.ssl.errors").increment();
        } else if (cause instanceof DecoderException) {
            meterRegistry.counter("mq.decode.errors").increment();
        }
        log.error("异常捕获：{}", cause.getMessage());
        ctx.close(); // 异常连接直接关闭
    }
}