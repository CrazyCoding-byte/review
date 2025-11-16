package com.yzx.crazycodingbytemq.store;

import com.yzx.crazycodingbytemq.model.MqMessage;

import java.util.concurrent.CompletableFuture;
import java.util.List;

/**
 * @className: MessageStoreStrategy
 * @author: yzx
 * @date: 2025/11/16 12:57
 * @Version: 1.0
 * @description:消息持久化策略接口
 */
public interface MessageStoreStrategy {
    /**
     * 保存消息
     * @param messageItem
     * @return
     */
    CompletableFuture<Boolean> save(MqMessage.MessageItem messageItem);

    /**
     * 批量保存消息
     * @param messageItems
     * @return
     */
    CompletableFuture<Boolean> batchSave(List<MqMessage.MessageItem> messageItems);

    /**
     * 删除消息
     * @param queueName
     * @param messageId
     * @return
     */
    CompletableFuture<Boolean> delete(String queueName, String messageId);

    // 物理删除过期文件（定时任务调用）
    void cleanExpiredFiles();

    /**
     * 加载队列消息
     * @param queueName
     * @return
     */
    List<MqMessage.MessageItem> loadQueueMessage(String queueName);

    // 崩溃恢复（服务重启时调用）
    CompletableFuture<RecoveryResult> recover();

    // 获取队列当前最大偏移量
    long getMaxOffset(String queueName);

    /**
     *关闭资源
     */
    void close();

    // 存储结果封装（包含偏移量、是否刷盘成功）
    record StoreResult(boolean success, long offset, String messageId, Throwable cause) {
    }

    record BatchStoreResult(boolean success, int successCount, long startOffset, Throwable cause) {
    }

    record MessageWithOffset(MqMessage.MessageItem message, long offset) {
    }

    record RecoveryResult(boolean success, int recoveredCount, int corruptedCount, String log) {
    }
}
