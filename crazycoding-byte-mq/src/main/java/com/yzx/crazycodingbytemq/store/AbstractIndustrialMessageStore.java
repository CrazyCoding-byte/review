package com.yzx.crazycodingbytemq.store;

import com.yzx.crazycodingbytemq.config.MessageStoreConfig;
import com.yzx.crazycodingbytemq.model.MqMessage;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.zip.CRC32;

/**
 * @className: AbstractIndustrialMessageStore
 * @author: yzx
 * @date: 2025/11/16 14:15
 * @Version: 1.0
 * @description:
 */
@Slf4j
public abstract class AbstractIndustrialMessageStore implements MessageStoreStrategy {
    protected final MessageStoreConfig config;
    // 校验算法实例
    protected final MessageDigest md5Digest;
    protected final CRC32 crc32 = new CRC32();

    public AbstractIndustrialMessageStore(MessageStoreConfig config) {
        this.config = config;
        this.md5Digest = initMd5Digest();
        // 初始化存储目录、清理过期文件线程
        initStoreDir();
        initCleanupScheduler();
    }

    // 初始化MD5摘要算法
    private MessageDigest initMd5Digest() {
        if (config.getChecksumAlgorithm() != MessageStoreConfig.ChecksumAlgorithm.MD5) {
            return null;
        }
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            log.error("初始化MD5算法失败，降级为CRC32", e);
            config.setChecksumAlgorithm(MessageStoreConfig.ChecksumAlgorithm.CRC32);
            return null;
        }
    }

    //计算消息校验和
    protected byte[] calculateCheckSum(MqMessage.MessageItem messageItem) {
        byte[] byteArray = messageItem.toByteArray();
        if (config.getChecksumAlgorithm() == MessageStoreConfig.ChecksumAlgorithm.MD5) {
            synchronized (md5Digest) {
                return md5Digest.digest(byteArray);
            }
        } else {
            synchronized (crc32) {
                crc32.reset();
                crc32.update(byteArray);
                long value = crc32.getValue();
                byte[] checksum = new byte[8];
                for (int i = 0; i < 8; i++) {
                    checksum[i] = (byte) (value >> (8 * i));
                }
                return checksum;
            }
        }
    }

    //验证校验和
    protected boolean verifyChecksum(MqMessage.MessageItem messageItem, byte[] checksum) {
        byte[] calculated = calculateCheckSum(messageItem);
        if (calculated.length != checksum.length) {
            return false;
        }
        for (int i = 0; i < calculated.length; i++) {
            if (calculated[i] != checksum[i]) {
                return false;
            }
        }
        return true;

    }

    // 初始化存储目录（工业级目录结构：baseDir/queueName/wal + baseDir/queueName/data）
    protected abstract void initStoreDir();

    // 初始化过期文件清理调度器（每天凌晨2点执行）
    protected abstract void initCleanupScheduler();

    // 刷盘操作（根据配置的刷盘策略）
    protected abstract CompletableFuture<Boolean> flushBuffer();
}
