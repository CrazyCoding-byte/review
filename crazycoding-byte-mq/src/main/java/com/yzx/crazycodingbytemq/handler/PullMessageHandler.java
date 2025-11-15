package com.yzx.crazycodingbytemq.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.yzx.crazycodingbytemq.codec.ProtocolConstant;
import com.yzx.crazycodingbytemq.enums.MessageTypeEnum;
import com.yzx.crazycodingbytemq.model.MqMessage;
import com.yzx.crazycodingbytemq.server.QueueManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import com.yzx.crazycodingbytemq.codec.ProtocolFrame;

/**
 * @className: PullMessageHandler
 * @author: yzx
 * @date: 2025/11/15 18:57
 * @Version: 1.0
 * @description: 处理消费者拉取消息请求
 */
@Slf4j
public class PullMessageHandler extends ChannelInboundHandlerAdapter {
    private final QueueManager queueManager = QueueManager.getInstance();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ProtocolFrame frame)) {
            ctx.fireChannelRead(msg);
            return;
        }
        //仅处理拉取消息请求类型
        if (frame.getMessageType() != MessageTypeEnum.PULL_MESSAGE.getCode()) {
            ctx.fireChannelRead(msg);
            return;
        }
        try {
            //解析请求
            MqMessage.PullMessageRequest pullMessageRequest = MqMessage.PullMessageRequest.parseFrom(frame.getBody());
            // 2. 校验核心参数
            if (!validateRequest(pullMessageRequest)) {
                MqMessage.PullMessageResponse response = MqMessage.PullMessageResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("核心参数缺失（queueName/consumerClientId/batchSize不能为空）")
                        .build();
                sendResponse(ctx, response);
                return;
            }

            // 3. 从队列拉取消息
            MqMessage.PullMessageResponse response = queueManager.pullMessage(pullMessageRequest);

            // 4. 发送响应
            sendResponse(ctx, response);

        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    // 校验核心参数
    private boolean validateRequest(MqMessage.PullMessageRequest request) {
        return request.getQueueName() != null && !request.getQueueName().trim().isEmpty()
                && request.getConsumerClientId() != null && !request.getConsumerClientId().trim().isEmpty()
                && request.getBatchSize() > 0;
    }

    // 发送响应
    private void sendResponse(ChannelHandlerContext ctx, MqMessage.PullMessageResponse response) {
        ProtocolFrame responseFrame = new ProtocolFrame(
                ProtocolConstant.MAGIC,
                ProtocolConstant.Version,
                response.toByteArray().length,
                MessageTypeEnum.PULL_MESSAGE_RESPONSE.getCode(),
                response.toByteArray()
        );

        ctx.writeAndFlush(responseFrame);
    }
}
