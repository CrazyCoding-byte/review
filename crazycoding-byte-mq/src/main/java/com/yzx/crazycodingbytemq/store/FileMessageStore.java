package com.yzx.crazycodingbytemq.store;

import com.yzx.crazycodingbytemq.model.MqMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @className: FileMessageStore
 * @author: yzx
 * @date: 2025/11/16 13:17
 * @Version: 1.0
 * @description: 文件存储策略
 */
@Slf4j
public class FileMessageStore extends AbstractMessageStore {
    private final String baseDir;//存储目录

    public FileMessageStore(String baseDir) {
        this.baseDir = baseDir;
        initDir();
    }

    private void initDir() {
        //创建目录
        Path path = Paths.get(baseDir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("文件存储目录初始化成功:{}", baseDir);
            } catch (IOException e) {
                throw new RuntimeException("初始化文件失败", e);
            }
        }
    }

    //获取队列对应的文件路径
    private String getQueueFilePath(String queueName) {
        return baseDir + File.separator + queueName + ".msg";
    }

    @Override
    protected void doSave(MqMessage.MessageItem message) throws Exception {
        String queueFilePath = getQueueFilePath(message.getQueueName());
        //追加写入消息(格式:校验和|消息体)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(queueFilePath, true))) {
            long checksum = calculateChecksum(message);
            writer.write(checksum + "|" + message.getMessageBody());
        }
    }

    @Override
    public CompletableFuture<Boolean> batchSave(List<MqMessage.MessageItem> messageItems) {

        return CompletableFuture.supplyAsync(() -> {
            if (messageItems.isEmpty()) return true;
            String queueFilePath = getQueueFilePath(messageItems.get(0).getQueueName());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(queueFilePath, true))) {
                for (MqMessage.MessageItem msg : messageItems) {
                    long checksum = calculateChecksum(msg);
                    writer.write(checksum + "|" + msg.toByteArray().toString() + "\n");
                }
                return true;
            } catch (Exception e) {
                log.error("批量保存消息失败: messageId={}", messageItems.get(0).getMessageId(), e);
                return false;
            }
        }, storeExecutor);
    }

    @Override
    public CompletableFuture<Boolean> delete(String queueName, String messageId) {
        //简化实现:实际应通过临时文件重写(跳过待删除消息)

        return CompletableFuture.supplyAsync(() -> {
            log.info("文件存储删除消息:queueName={},messageId={}", queueName, messageId);
            return true;
        }, storeExecutor);
    }

    @Override
    public List<MqMessage.MessageItem> loadQueueMessage(String queueName) {
        List<MqMessage.MessageItem> messageItems = new ArrayList<>();
        String queueFilePath = getQueueFilePath(queueName);
        File file = new File(queueFilePath);
        if (!file.exists()) return messageItems;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) continue;
                long checksum = Long.parseLong(parts[0]);
                MqMessage.MessageItem messageItem = MqMessage.MessageItem.parseFrom(parts[1].getBytes());
                //校验和验证
                if (calculateChecksum(messageItem) != checksum) {
                    log.warn("文件存储消息校验和错误:queueName={},messageId={}", queueName, messageItem.getMessageId());
                    continue;
                }
                messageItems.add(messageItem);
            }
            log.info("从文件加载队列消息成功: queueName={}, 数量={}", queueName, messageItems.size());
        } catch (FileNotFoundException e) {
            log.error("加载队列消息失败: queueName={}", queueName, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("加载队列消息失败: queueName={}", queueName, e);
            throw new RuntimeException(e);
        }
        return messageItems;
    }
}
