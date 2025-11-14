package com.yzx.crazycodingbytegateway.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import java.nio.channels.SocketChannel;

/**
 * @className: GatewayChannelInitializer
 * @author: yzx
 * @date: 2025/11/14 1:15
 * @Version: 1.0
 * @description:
 */
public class GatewayChannelInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipline();
        //http 编解码
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65535);
        //自定义网关
        pipeline.addLast(new GatewayHandler());
    }
}
