package com.yzx.crazycodingbytemq.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.yzx.crazycodingbytemq.codec.ProtocolConstant;
import com.yzx.crazycodingbytemq.enums.MessageTypeEnum;
import com.yzx.crazycodingbytemq.model.MqMessage;
import com.yzx.crazycodingbytemq.server.QueueManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import com.yzx.crazycodingbytemq.codec.ProtocolFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * @className: SendMessageHandler
 * @author: yzx
 * @date: 2025/11/15 18:37
 * @Version: 1.0
 * @description: 处理生产者发送消息请求
 */
@Slf4j
public class SendMessageHandler extends ChannelInboundHandlerAdapter {
    private final QueueManager queueManager = QueueManager.getInstance();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ProtocolFrame frame)) {
            ctx.fireChannelRead(msg);
            return;
        }
        //仅仅处理发送消息请求类型
        if (frame.getMessageType() != MessageTypeEnum.SEND_MESSAGE.getCode()) {
            ctx.fireChannelRead(msg);
            return;
        }
        try {
            //解析请求
            MqMessage.SendMessageRequest request = MqMessage.SendMessageRequest.parseFrom(frame.getBody());
            //校验参数
            if (!validMessage(request)) {
                sendResponse(ctx, request.getMessageId(), false, "核心参数缺失（queueName/messageId/messageBody不能为空）");
                return;
            }
            //消息入队
            boolean b = queueManager.sendMessage(request);
            //发送响应
            String responseMessage = b ? "发送成功" : "发送失败";
            sendResponse(ctx, request.getMessageId(), b, responseMessage);
        } catch (InvalidProtocolBufferException e) {
            log.error("解析发送消息请求失败", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("发送消息失败", e);
            sendResponse(ctx, null, false, "发送消息失败");
        }

    }


    //校验核心参数
    private boolean validMessage(MqMessage.SendMessageRequest request) {
        return request.getQueueName() != null && request.getQueueName().trim().isEmpty()
                && request.getMessageId() != null && request.getMessageId().trim().isEmpty()
                && request.getMessageBody() != null && request.getMessageBody().trim().isEmpty();
    }

    //发送响应
    private void sendResponse(ChannelHandlerContext ctx, String messageId, boolean success, String msg) {
        MqMessage.SendMessageResponse response = MqMessage.SendMessageResponse.newBuilder()
                .setSuccess(success)
                .setMessage(msg)
                .setMessageId(messageId == null ? "" : messageId)
                .build();

        ProtocolFrame responseFrame = new ProtocolFrame(
                ProtocolConstant.MAGIC,
                ProtocolConstant.Version,
                response.toByteArray().length,
                MessageTypeEnum.SEND_MESSAGE_RESPONSE.getCode(),
                response.toByteArray()
        );

        ctx.writeAndFlush(responseFrame);
    }
}
