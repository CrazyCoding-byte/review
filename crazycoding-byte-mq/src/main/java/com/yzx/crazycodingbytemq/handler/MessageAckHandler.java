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
 * @className: MessageAckHandler
 * @author: yzx
 * @date: 2025/11/15 18:58
 * @Version: 1.0
 * @description:处理信息消费确认请求
 */
@Slf4j
public class MessageAckHandler extends ChannelInboundHandlerAdapter {
    private final QueueManager queueManager = QueueManager.getInstance();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ProtocolFrame frame)) {
            ctx.fireChannelRead(msg);
            return;
        }
        // 仅处理"消息确认请求"类型
        if (frame.getMessageType() != MessageTypeEnum.MESSAGE_ACK.getCode()) {
            ctx.fireChannelRead(msg);
            return;
        }

        try {
            // 1. 解析请求
            MqMessage.MessageAckRequest request = MqMessage.MessageAckRequest.parseFrom(frame.getBody());

            // 2. 校验核心参数
            if (!validateRequest(request)) {
                MqMessage.MessageAckResponse response = MqMessage.MessageAckResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("核心参数缺失（queueName/consumerClientId/messageId不能为空）")
                        .setMessageId(request.getMessageId() == null ? "" : request.getMessageId())
                        .build();
                sendResponse(ctx, response);
                return;
            }

            // 3. 处理确认逻辑
            MqMessage.MessageAckResponse response = queueManager.ackMessage(request);

            // 4. 发送响应
            sendResponse(ctx, response);

        } catch (InvalidProtocolBufferException e) {
            log.error("解析消息确认请求失败", e);
            MqMessage.MessageAckResponse response = MqMessage.MessageAckResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("请求格式非法")
                    .setMessageId("")
                    .build();
            sendResponse(ctx, response);
        } catch (Exception e) {
            log.error("处理消息确认请求时发生未知错误", e);
            MqMessage.MessageAckResponse response = MqMessage.MessageAckResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("服务器内部错误")
                    .setMessageId("")
                    .build();
            sendResponse(ctx, response);
        }
    }

    private boolean validateRequest(MqMessage.MessageAckRequest request) {
        return request.getQueueName() != null && !request.getQueueName().trim().isEmpty()
                && request.getConsumerClientId() != null && !request.getConsumerClientId().trim().isEmpty()
                && request.getMessageId() != null && !request.getMessageId().trim().isEmpty();
    }

    private void sendResponse(ChannelHandlerContext ctx, MqMessage.MessageAckResponse response) {
        ProtocolFrame responseFrame = new ProtocolFrame(
                ProtocolConstant.MAGIC,
                ProtocolConstant.Version,
                response.toByteArray().length,
                MessageTypeEnum.MESSAGE_ACK_RESPONSE.getCode(),
                response.toByteArray()
        );

        ctx.writeAndFlush(responseFrame);
    }
}
