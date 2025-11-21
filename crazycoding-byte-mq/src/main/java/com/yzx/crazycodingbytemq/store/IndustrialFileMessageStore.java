package com.yzx.crazycodingbytemq.store;

import com.google.protobuf.InvalidProtocolBufferException;
import com.yzx.crazycodingbytemq.codec.ProtocolConstant;
import com.yzx.crazycodingbytemq.config.MessageStoreConfig;
import com.yzx.crazycodingbytemq.model.MqMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yzx.crazycodingbytemq.codec.ProtocolFrame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static com.sun.org.apache.xml.internal.serializer.utils.Utils.messages;

/**
 * @className: IndustrialFileMessageStore
 * @author: yzx
 * @date: 2025/11/16 14:53
 * @Version: 1.0
 * @description:
 */
@Slf4j
public class IndustrialFileMessageStore extends AbstractIndustrialMessageStore {
    //队列->存储上下文()
    private final Map<String, QueueStoreContext> queueContexts = new ConcurrentHashMap<>();
    //批量刷盘任务调度器
    private final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "store-flush-scheduler"));
    //过期文件清理调度器
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "store-cleanup-scheduler"));
    //全局写锁(保证同一队列的写操作原子性)
    private final ReentrantLock globalWriteLock = new ReentrantLock();
    // 内存缓冲区（按队列划分）
    private final Map<String, ByteBuffer> queueBuffers = new ConcurrentHashMap<>();
    // 批量刷盘计数器（按队列统计）
    private final Map<String, AtomicLong> batchCounter = new ConcurrentHashMap<>();
    // 存储文件命名格式：queueName-yyyyMMdd-HHmmss-序号.log
    private static final String FILE_NAME_PATTERN = "%s-%s-%d.log";

    // 存储格式：[传输层帧头] + [存储层扩展字段] + [传输层帧体]
    // 传输层帧头：魔数(4) + 版本(1) + 消息体长度(4) + 消息类型(1)
    // 存储层扩展：偏移量(8) + 校验和(16)
    // 传输层帧体：消息体(N)
    // 尾部校验：TRAILER_MAGIC(4)
    public IndustrialFileMessageStore(MessageStoreConfig config) {
        super(config);
        // 启动批量刷盘定时任务
        startBatchFlushScheduler();
    }

    // 启动批量刷盘调度器（按超时时间触发）
    private void startBatchFlushScheduler() {
        flushScheduler.scheduleAtFixedRate(
                this::flushAllQueuesBuffer,
                config.getBatchFlushTimeout().toMillis(),
                config.getBatchFlushTimeout().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    protected void initStoreDir() {
        Path basePath = Paths.get(config.getBaseDir());
        try {
            Files.createDirectories(basePath);
            log.info("工业级存储目录初始化成功：{}", basePath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("存储目录初始化失败，无法启动存储服务", e);
        }
    }

    @Override
    protected void initCleanupScheduler() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 2);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long initialDelay = calendar.getTimeInMillis() - System.currentTimeMillis();
        initialDelay = initialDelay < 0 ? initialDelay + 24 * 3600 * 1000 : initialDelay;
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanExpiredFiles,
                initialDelay,
                24 * 3600 * 1000,
                TimeUnit.MILLISECONDS
        );
    }

    /*
     *刷盘操作当缓存区数据不够时候，将数据写入磁盘文件，并清空缓存区。
     */
    @Override
    protected CompletableFuture<Boolean> flushBuffer(String queueName) {
        return CompletableFuture.supplyAsync(() -> {
            ByteBuffer buffer = queueBuffers.get(queueName);
            if (buffer == null || buffer.position() == 0) return true;
            try {
                QueueStoreContext context = queueContexts.get(queueName);
                if (context.dataChannel.size() >= config.getMaxFileSize()) {
                    rotateWALFile(context);
                }
                buffer.flip();
                context.dataChannel.write(buffer);
                if (config.getFlushPolicy() == MessageStoreConfig.FlushPolicy.SYNC) {
                    context.dataChannel.force(true);
                }
                buffer.clear();
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void rotateDateFile(QueueStoreContext context) throws IOException {
        context.dataChannel.force(true);
        context.dataChannel.close();
        String timestamp = LocalDate.now().toString().replace("-", "");
        long seq = context.dataFileSeq.incrementAndGet();
        Path newDataFile = context.dataDir.resolve(String.format(FILE_NAME_PATTERN, context.queueName, timestamp, seq));
        context.dataChannel = FileChannel.open(newDataFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    @Override
    public CompletableFuture<MessageStoreStrategy.StoreResult> save(ProtocolFrame protocolFrame, String messageId) {
        return CompletableFuture.supplyAsync(() -> {
            globalWriteLock.lock();
            try {
                MqMessage.MessageItem messageItem = MqMessage.MessageItem.parseFrom(protocolFrame.getBody());
                String queueName = messageItem.getQueueName();
                QueueStoreContext context = getOrCreateQueueContext(queueName);
                ByteBuffer buffer = getOrCreateQueueBuffer(queueName);
                AtomicLong counter = getOrCreateBatchCounter(queueName);
                //1.生成存储偏移量
                long offset = context.maxOffset.incrementAndGet();
                //2.计算校验和(覆盖整个ProtocolFrame+偏移量)
                byte[] checksum = calculateFrameChecksum(protocolFrame, offset);
                //3.写入内存缓冲区
                writeToBuffer(buffer, offset, protocolFrame, checksum);
                //4.写入WAL文件日志(与传输层格式一致,便于恢复)
                writeToWAL(context, offset, protocolFrame, checksum);
                //5.检查批量刷盘条件
                if (counter.incrementAndGet() >= config.getBatchFlushThreshold()) {
                    flushBuffer(queueName).join();
                    counter.set(0);
                }
                return new MessageStoreStrategy.StoreResult(true, offset, messageId, null);
            } catch (InvalidProtocolBufferException e) {
                log.error("存储ProtocolFrame失败", e);
                return new MessageStoreStrategy.StoreResult(false, -1, messageId, e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                globalWriteLock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<MessageStoreStrategy.BatchStoreResult> batchSave(List<MqMessage.MessageItem> messageItems) {
        //批量存储时间基于ProtocolFrame构建
        return CompletableFuture.supplyAsync(() -> {
            globalWriteLock.lock();
            try {
                if (messageItems.isEmpty()) {
                    return new MessageStoreStrategy.BatchStoreResult(true, 0, -1, null);
                }
                MqMessage.MessageItem messageItem = messageItems.get(0);
                String queueName = messageItem.getQueueName();
                QueueStoreContext context = getOrCreateQueueContext(queueName);
                long startOffset = context.maxOffset.get() + 1;
                int successCount = 0;
                for (MqMessage.MessageItem msg : messageItems) {
                    try {
                        ProtocolFrame protocolFrame = new ProtocolFrame(ProtocolConstant.MAGIC,
                                ProtocolConstant.Version, msg.toByteArray().length, (byte) 0x01, msg.toByteArray());
                        //复用单条存储逻辑
                        save(protocolFrame, msg.getMessageId()).join();
                        successCount++;
                    } catch (Exception e) {
                        log.error("存储ProtocolFrame失败", e);
                    }
                }
                return new MessageStoreStrategy.BatchStoreResult(
                        successCount == messageItems.size(),
                        successCount,
                        startOffset,
                        null
                );
            } finally {
                globalWriteLock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(String queueName, String messageId) {
        return null;
    }

    @Override
    public void cleanExpiredFiles() {
        LocalDate localDate = LocalDate.now().minusDays(config.getFileRetentionDays());
        Path basePath = Paths.get(config.getBaseDir());
        try (DirectoryStream<Path> queueDirs = Files.newDirectoryStream(basePath)) {
            for (Path dir : queueDirs) {
                if (Files.isDirectory(dir)) {
                    cleanExpiredFilesInDir(dir.resolve("wal"), localDate);
                    cleanExpiredFilesInDir(dir.resolve("data"), localDate);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 2. Files：Java 官方的 “文件操作工具箱”
     * 为什么用：替代File类的createNewFile()、listFiles()等方法，API 更简洁，支持批量操作、目录遍历。
     * 核心用法（MQ 中用到的）：
     * Files.createDirectories(path)：创建目录（如果父目录不存在，自动创建，不用写循环）；
     * Files.walkFileTree(path, 访问者)：遍历目录下所有文件（比如清理过期文件时用）；
     * Files.delete(file)：删除文件；
     * Files.newDirectoryStream(path)：遍历目录下的文件（比如读取所有 WAL 日志文件）。
     * @param dir
     * @param expireDate
     * @throws IOException
     */
    private void cleanExpiredFilesInDir(Path dir, LocalDate expireDate) throws IOException {
        if (!Files.exists(dir)) {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {


                }
            }
        }
    }

    @Override
    public List<MqMessage.MessageItem> loadQueueMessage(String queueName) {
        return List.of();
    }

    @Override
    public CompletableFuture<RecoveryResult> recover() {
        return null;
    }

    @Override
    public long getMaxOffset(String queueName) {
        return 0;
    }

    @Override
    public void close() {

    }

    /*
     *写入内存缓冲区
     */
    private void writeToBuffer(ByteBuffer buffer, long offset, ProtocolFrame frame, byte[] checksum) {
        // 存储格式说明（与ProtocolFrame对应）：
        // [ProtocolConstant.MAGIC(4字节)] → 与传输层魔数一致
        // [version(1字节)] → 与传输层版本一致
        //[messageType(1字节)] → 与传输层消息类型一致
        // [offset(8字节)] → 存储层特有（定位消息）
        // [bodyLength(4字节)] → 对应ProtocolFrame.bodyLength
        // [messageBytes(N字节)] → 对应ProtocolFrame.body（核心消息体）
        // [checksum(16字节)] → 存储层校验
        // [TRAILER_MAGIC(4字节)] → 存储层校验帧尾
        int requiredSize = ProtocolConstant.FRAME_HEADER_LENGTH  // 10字节完整帧头
                + 8  // offset
                + 16 // checksum
                + frame.getBodyLength()  // 消息体
                + 4; // TRAILER_MAGIC（4字节）
        if (buffer.remaining() < requiredSize) {
            flushBuffer(buffer.toString()).join();
        }

        //1.写入传输层帧头(与网络传输格式完全一致)
        buffer.putInt(ProtocolConstant.MAGIC); // 复用传输层魔数（关键）
        buffer.put(frame.getVersion());
        buffer.putInt(frame.getBodyLength());
        buffer.put(frame.getMessageType());
        //2.写入存储层扩展字段
        buffer.putLong(offset);
        buffer.put(checksum);
        //3.写入传输层帧体
        buffer.put(frame.getBody()); // 对应ProtocolFrame.body
        //4.写入尾部校验帧尾魔术
        buffer.putInt(ProtocolConstant.TRAILER_MAGIC);
    }

    /**ByteBuffer.allocate(容量)：创建指定大小的缓冲区；
     buffer.putInt(值)/putLong(值)/put(字节数组)：往缓冲区写数据；
     buffer.flip()：切换为 “读模式”（写完后，告诉缓冲区 “现在要从开头读了”）；
     buffer.clear()：清空缓冲区，准备下次写；
     buffer.remaining()：判断缓冲区还剩多少可用空间。
     *写入WAL预写日志
     */
    private void writeToWAL(QueueStoreContext queueContext, long offset, ProtocolFrame frame, byte[] checksum) throws
            IOException {
        //检查WAL文件大小,触发轮转
        if (queueContext.walChannel.size() >= config.getMaxFileSize()) {
            rotateWALFile(queueContext);
        }
        //构建与内存缓冲区一致的字节序列
        ByteBuffer walBuffer = ByteBuffer.allocate(ProtocolConstant.FRAME_HEADER_LENGTH + 8 + 16 + frame.getBodyLength() + 4);
        // 传输层帧头
        walBuffer.putInt(frame.getMagic());
        walBuffer.put(frame.getVersion());
        walBuffer.putInt(frame.getBodyLength());
        walBuffer.put(frame.getMessageType());
        // 存储扩展字段
        walBuffer.putLong(offset);
        walBuffer.put(checksum);
        // 传输层帧体
        walBuffer.put(frame.getBody());
        // 尾部魔数
        walBuffer.putInt(ProtocolConstant.TRAILER_MAGIC);
        walBuffer.flip();
        // 写入WAL并根据策略刷盘
        queueContext.walChannel.write(walBuffer);
        if (config.getFlushPolicy() == MessageStoreConfig.FlushPolicy.SYNC) {
            queueContext.walChannel.force(true);
        }
    }


    private void rotateWALFile(QueueStoreContext context) throws IOException {
        context.walChannel.force(true);
        context.walChannel.close();
        String timestamp = LocalDate.now().toString().replace("-", "");
        long seq = context.walFileSeq.incrementAndGet();
        Path newWalFile = context.walDir.resolve(
                String.format(FILE_NAME_PATTERN, context.queueName, timestamp, seq)
        );
        context.walChannel = FileChannel.open(newWalFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    /**
     * 从存储文件回复ProtocolFrame
     */
    private ProtocolFrame recoverFrame(ByteBuffer buffer) {
        //1.读取传输层帧头
        int magic = buffer.getInt();
        if (magic != ProtocolConstant.MAGIC) {
            log.error("非法帧头:{}", magic);
            return null;
        }
        byte version = buffer.get();
        int bodyLength = buffer.getInt();
        byte messageType = buffer.get();
        //2.跳过存储扩展字段
        buffer.position(buffer.position() + 8 + 16);
        //3.读取传输层帧体
        byte[] body = new byte[bodyLength];
        buffer.get(body);
        //4.验证尾部魔数
        int trailerMagic = buffer.getInt();
        if (trailerMagic != ProtocolConstant.TRAILER_MAGIC) {
            log.error("尾部魔数不匹配，帧损坏");
            return null;
        }
        return new ProtocolFrame(magic, version, bodyLength, messageType, body);
    }


    private ByteBuffer getOrCreateQueueBuffer(String queueName) {
        return queueBuffers.computeIfAbsent(queueName, name -> ByteBuffer.allocate(config.getBufferSize()));
    }

    private AtomicLong getOrCreateBatchCounter(String queueName) {
        return batchCounter.computeIfAbsent(queueName, name -> new AtomicLong(0));
    }

    private QueueStoreContext getOrCreateQueueContext(String queueName) {
        return queueContexts.computeIfAbsent(queueName, name -> {
            try {
                Path queueDir = Paths.get(config.getBaseDir(), queueName);
                Path walDir = queueDir.resolve("wal");
                Path dataDir = queueDir.resolve("data");
                Files.createDirectories(walDir);
                Files.createDirectories(dataDir);

                //初始化当前WAL文件
                String timestamp = LocalDate.now().toString().replace("-", "");
                String walFileName = String.format(FILE_NAME_PATTERN, queueName, name, 1);
                Path walFilePath = walDir.resolve(walFileName);
                FileChannel walChannel = FileChannel.open(walFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                //初始化当前数据文件
                String dataFilename = String.format(FILE_NAME_PATTERN, name, timestamp, 1);
                Path dataFile = dataDir.resolve(dataFilename);
                FileChannel dataChannel = FileChannel.open(dataFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                return new QueueStoreContext(
                        name, walDir, dataDir, walChannel, dataChannel,
                        new AtomicLong(0), new AtomicLong(1), new AtomicLong(1)
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    //计算出16字节的校验和 根据传输层帧头+存储偏移量+传输层帧体
    private byte[] calculateFrameChecksum(ProtocolFrame frame, long offset) {
        //校验范围:传输层帧头+存储偏移量+传输层帧体
        ByteBuffer checkBuffer = ByteBuffer.allocate(ProtocolConstant.FRAME_HEADER_LENGTH + 8 + frame.getBodyLength());
        checkBuffer.putInt(frame.getMagic());
        checkBuffer.put(frame.getVersion());
        checkBuffer.putInt(frame.getBodyLength());
        checkBuffer.put(frame.getMessageType());
        checkBuffer.putLong(offset);
        checkBuffer.put(frame.getBody());
        checkBuffer.flip();
        return calculateCheckSum(checkBuffer.array());
    }

    private void cleanExpireFilesInDir(Path dir, LocalDate expire) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                LocalDate createDate = Instant.ofEpochMilli(attrs.creationTime().toMillis())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                if (createDate.isBefore(expire)) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 队列存储上下文（封装队列的所有存储相关资源）
     */
    @Data
    private class QueueStoreContext {  // 改为普通class
        private final String queueName;          // 队列名（不变的字段仍可保留final）
        private final Path walDir;               // WAL日志目录（不变的字段仍可保留final）
        private final Path dataDir;              // 数据文件目录（不变的字段仍可保留final）
        private FileChannel walChannel;          // 改为非final，允许重新赋值
        private FileChannel dataChannel;         // 改为非final，允许重新赋值
        private final AtomicLong maxOffset;      // 当前最大偏移量（Atomic本身可变，可保留final）
        private final AtomicLong walFileSeq;     // WAL文件序号（Atomic本身可变，可保留final）
        private final AtomicLong dataFileSeq;    // 数据文件序号（Atomic本身可变，可保留final）

        // 构造器（保留原有的初始化逻辑）
        public QueueStoreContext(String queueName, Path walDir, Path dataDir,
                                 FileChannel walChannel, FileChannel dataChannel,
                                 AtomicLong maxOffset, AtomicLong walFileSeq, AtomicLong dataFileSeq) {
            this.queueName = queueName;
            this.walDir = walDir;
            this.dataDir = dataDir;
            this.walChannel = walChannel;
            this.dataChannel = dataChannel;
            this.maxOffset = maxOffset;
            // 初始化文件序号（如果为null则默认从1开始）
            this.walFileSeq = (walFileSeq == null) ? new AtomicLong(1) : walFileSeq;
            this.dataFileSeq = (dataFileSeq == null) ? new AtomicLong(1) : dataFileSeq;
        }

    }
}
