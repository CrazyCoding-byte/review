package com.yzx.crazycodingbyteorder.config;

import com.yzx.crazycodingbyteorder.entity.LocalMessage;
import com.yzx.crazycodingbyteorder.mapper.LocalMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCompensator {
    
    private final LocalMessageMapper localMessageMapper;
    private final InventoryServiceClient inventoryServiceClient; // 假设这是一个HTTP客户端
    
    /**
     * 定时任务：每30秒执行一次，补偿发送失败的消息
     * 这是本地消息表的核心：保证最终一致性
     */
    @Scheduled(fixedDelay = 30000) // 30秒
    public void compensateMessages() {
        log.info("开始补偿发送消息...");
        
        try {
            // 1. 查询待发送的消息（每次最多处理100条）
            List<LocalMessage> pendingMessages = localMessageMapper.selectPendingMessages(100);
            
            if (pendingMessages.isEmpty()) {
                log.debug("没有待发送的消息");
                return;
            }
            
            log.info("发现 {} 条待发送的消息", pendingMessages.size());
            
            // 2. 逐条处理消息
            for (LocalMessage message : pendingMessages) {
                try {
                    // 3. 发送消息到库存服务
                    boolean success = sendToInventoryService(message);
                    
                    if (success) {
                        // 发送成功，更新状态
                        localMessageMapper.markAsSent(message.getMessageId());
                        log.info("消息发送成功，消息ID：{}，业务ID：{}", 
                                message.getMessageId(), message.getBusinessId());
                    } else {
                        // 发送失败，更新重试次数
                        localMessageMapper.updateMessageStatus(
                            message.getMessageId(),
                            0, // 重置为待发送
                            "发送失败，等待重试"
                        );
                        log.warn("消息发送失败，消息ID：{}，业务ID：{}", 
                                message.getMessageId(), message.getBusinessId());
                    }
                    
                } catch (Exception e) {
                    log.error("处理消息失败，消息ID：{}", message.getMessageId(), e);
                    
                    // 更新错误信息
                    localMessageMapper.updateMessageStatus(
                        message.getMessageId(),
                        0,
                        "处理异常: " + e.getMessage()
                    );
                }
            }
            
        } catch (Exception e) {
            log.error("补偿任务执行失败", e);
        }
    }
    
    /**
     * 发送消息到库存服务（模拟HTTP调用）
     */
    private boolean sendToInventoryService(LocalMessage message) {
        try {
            // 这里模拟HTTP调用库存服务
            // 实际项目中：使用RestTemplate或Feign调用库存服务
            
            log.info("发送消息到库存服务，消息内容：{}", message.getPayload());
            
            // 模拟调用
            // String url = "http://localhost:8083/api/inventory/lock";
            // ResponseEntity<String> response = restTemplate.postForEntity(url, message.getPayload(), String.class);
            // return response.getStatusCode().is2xxSuccessful();
            
            // 为了演示，这里直接返回true
            return true;
            
        } catch (Exception e) {
            log.error("调用库存服务失败", e);
            return false;
        }
    }
}