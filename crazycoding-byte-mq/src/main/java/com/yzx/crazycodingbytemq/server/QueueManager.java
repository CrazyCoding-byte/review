package com.yzx.crazycodingbytemq.server;

import com.yzx.crazycodingbytemq.model.MqMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @className: QueueManager
 * @author: yzx
 * @date: 2025/11/15 18:15
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class QueueManager {
    private static final QueueManager INSTANCE = new QueueManager();
    //队列存储: queueName->消息队列
    private final Map<String, ConcurrentLinkedQueue<MqMessage.MessageItem>> queueMap = new ConcurrentHashMap<>();

    public static QueueManager getInstance() {
        return INSTANCE;
    }

    /**
     * 向队列发送消息(生产者调用)
     */
    public boolean sendMessage(MqMessage.SendMessageRequest request) {
        try {
            //不存在则创建队列(懒加载)
            ConcurrentLinkedQueue<MqMessage.MessageItem> queue = queueMap.computeIfAbsent(request.getQueueName(), k -> new ConcurrentLinkedQueue<>());
            //构建MessageItem
            MqMessage.MessageItem messageItem = MqMessage.MessageItem.newBuilder()
                    .setMessageId(request.getMessageId())
                    .setMessageBody(request.getMessageBody())
                    .setQueueName(request.getQueueName())
                    .setPriority(request.getPriority())
                    .build();
            //入队
            queue.offer(messageItem);
            log.info("消息入队成功：queueName={}, messageId={}, 队列长度={}",
                    request.getQueueName(), request.getMessageId(), queue.size());
            return true;

        } catch (Exception e) {
            log.error("消息入队失败：queueName={}, messageId={}",
                    request.getQueueName(), request.getMessageId(), e);
            return false;
        }
    }

    /**
     * 从队列拉取消息(消费者调用)
     */
    public MqMessage.PullMessageResponse pullMessage(MqMessage.PullMessageRequest request) {
        //校验批量大小(最大100)
        if (request.getBatchSize() < 0 || request.getBatchSize() > 100) {
            return MqMessage.PullMessageResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("批量拉取数量非法（1-100）")
                    .build();
        }
        //获取队列(不存在则返回空)
        ConcurrentLinkedQueue<MqMessage.MessageItem> queue = queueMap.get(request.getQueueName());
        if (queue == null || queue.isEmpty()) {
            return MqMessage.PullMessageResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("队列不存在或已空")
                    .build();
        }
        //批量拉取消息
        MqMessage.PullMessageResponse.Builder responseBuilder = MqMessage.PullMessageResponse.newBuilder();
        responseBuilder.setSuccess(true)
                .setMessage("拉取成功");
        int pullCount = 0;
        while (pullCount < request.getBatchSize()) {
            MqMessage.MessageItem messageItem = queue.poll();
            if (messageItem == null) break; //队列为空则退出
            responseBuilder.addMessageList(messageItem);
            pullCount++;
        }
        log.info("从队列拉取消息成功：queueName={}, 拉取数量={}", request.getQueueName(), pullCount);
        return responseBuilder.build();
    }

    /**
     * 消息消费确认(暂未实现持久化)
     */
    public MqMessage.MessageAckResponse ackMessage(MqMessage.MessageAckRequest request) {
        log.info("消息确认成功：queueName={}, messageId={}", request.getQueueName(), request.getMessageId());
        return MqMessage.MessageAckResponse.newBuilder()
                .setSuccess(true)
                .setMessage("确认成功")
                .setMessageId(request.getMessageId())
                .build();
    }
}
