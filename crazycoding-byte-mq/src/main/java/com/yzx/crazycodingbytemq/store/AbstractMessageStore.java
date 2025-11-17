package com.yzx.crazycodingbytemq.store;

import com.yzx.crazycodingbytemq.model.MqMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @className: AbstractMessageStore
 * @author: yzx
 * @date: 2025/11/16 13:03
 * @Version: 1.0
 * @description:持久化策略抽象类(实现通用逻辑)
 */
@Slf4j
public abstract class AbstractMessageStore implements MessageStoreStrategy {
    // 异步执行器（所有策略共享）
    protected final ExecutorService storeExecutor = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "message-store-worker")
    );

    @Override
    public CompletableFuture<Boolean> save(MqMessage.MessageItem message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                doSave(message);
                return true;
            } catch (Exception e) {
                log.error("保存消息失败: messageId={}", message.getMessageId(), e);
                return false;
            }
        }, storeExecutor);
    }

    // 子类实现具体保存逻辑
    protected abstract void doSave(MqMessage.MessageItem message) throws Exception;

    @Override
    public void close() {
        storeExecutor.shutdown();
        log.info("持久化线程池已关闭");
    }
}
