// wms/src/main/java/com/yzx/crazycodingbytemms/service/impl/InventoryServiceImpl.java
package com.yzx.crazycodingbytewms.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzx.crazycodingbytecommon.entity.BusinessException;
import com.yzx.crazycodingbytewms.dto.InventoryLockDTO;
import com.yzx.crazycodingbytewms.entity.Inventory;
import com.yzx.crazycodingbytewms.entity.InventoryLockLog;
import com.yzx.crazycodingbytewms.entity.InventoryOperationLog;
import com.yzx.crazycodingbytewms.mapper.InventoryLockLogMapper;
import com.yzx.crazycodingbytewms.mapper.InventoryMapper;
import com.yzx.crazycodingbytewms.mapper.InventoryOperationLogMapper;
import com.yzx.crazycodingbytewms.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 库存Service实现（适配你的三张表）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryLockLogMapper inventoryLockLogMapper;
    private final InventoryOperationLogMapper inventoryOperationLogMapper;

    /**
     * 锁库存核心逻辑：
     * 1. 行锁查询库存，防止并发
     * 2. 乐观锁扣减可用库存+增加锁定库存
     * 3. 记录锁定日志（30分钟过期）
     * 4. 记录操作日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockStock(InventoryLockDTO lockDTO) {
        log.info("开始锁库存，订单号：{}，商品ID：{}，锁定数量：{}",
                lockDTO.getOrderNo(), lockDTO.getProductId(), lockDTO.getQuantity());

        // 1. 行锁查询库存（防止多线程同时修改同个商品库存）
        Inventory inventory = inventoryMapper.selectByProductIdForUpdate(lockDTO.getProductId());
        if (inventory == null) {
            log.error("商品不存在，商品ID：{}", lockDTO.getProductId());
            return false;
        }
        if (inventory.getAvailableStock() < lockDTO.getQuantity()) {
            log.warn("可用库存不足，商品ID：{}，可用：{}，请求锁定：{}",
                    inventory.getProductId(), inventory.getAvailableStock(), lockDTO.getQuantity());
            return false;
        }

        // 2. 乐观锁锁定库存（扣可用+加锁定）
        int lockResult = inventoryMapper.lockStockWithOptimisticLock(
                lockDTO.getProductId(),
                lockDTO.getQuantity(),
                inventory.getVersion()
        );
        if (lockResult <= 0) {
            log.warn("锁库存失败（乐观锁冲突/库存不足），商品ID：{}，版本号：{}",
                    lockDTO.getProductId(), inventory.getVersion());
            return false;
        }

        // 3. 生成锁定流水号，记录锁定日志（30分钟过期）
        String lockNo = IdUtil.simpleUUID();
        InventoryLockLog lockLog = new InventoryLockLog();
        lockLog.setLockNo(lockNo);
        lockLog.setOrderNo(lockDTO.getOrderNo());
        lockLog.setProductId(lockDTO.getProductId());
        lockLog.setQuantity(lockDTO.getQuantity());
        lockLog.setStatus(0); // 0-已锁定
        lockLog.setLockTime(LocalDateTime.now());
        lockLog.setExpireTime(LocalDateTime.now().plusMinutes(30)); // 30分钟过期
        lockLog.setRemark("订单创建锁定库存，订单号：" + lockDTO.getOrderNo());

        int lockLogResult = inventoryLockLogMapper.insert(lockLog);
        if (lockLogResult <= 0) {
            log.error("记录库存锁定日志失败，订单号：{}", lockDTO.getOrderNo());
            throw new BusinessException("记录锁定日志失败，库存回滚");
        }

        // 4. 记录库存操作日志
        InventoryOperationLog operationLog = new InventoryOperationLog();
        operationLog.setProductId(lockDTO.getProductId());
        operationLog.setOperationType("LOCK"); // 锁定操作
        operationLog.setOrderNo(lockDTO.getOrderNo());
        operationLog.setBeforeStock(inventory.getAvailableStock()); // 操作前可用库存
        operationLog.setAfterStock(inventory.getAvailableStock() - lockDTO.getQuantity()); // 操作后可用库存
        operationLog.setChangeQuantity(-lockDTO.getQuantity()); // 变动量（扣减为负）
        operationLog.setOperatorId(lockDTO.getUserId()); // 操作人=下单用户ID
        operationLog.setRemark("订单" + lockDTO.getOrderNo() + "锁定库存");

        inventoryOperationLogMapper.insert(operationLog);

        log.info("锁库存成功，订单号：{}，锁定流水号：{}", lockDTO.getOrderNo(), lockNo);
        return true;
    }

    /**
     * 解锁库存（高并发安全版：幂等+校验+乐观锁）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlockStock(InventoryLockDTO unlockDTO) {
        log.info("开始解锁库存，订单号：{}，商品ID：{}，解锁数量：{}",
                unlockDTO.getOrderNo(), unlockDTO.getProductId(), unlockDTO.getQuantity());

        // ========== 保障1：幂等控制（查锁定日志，确保只解锁一次） ==========
        LambdaQueryWrapper<InventoryLockLog> inventoryLockLogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        inventoryLockLogLambdaQueryWrapper.eq(InventoryLockLog::getOrderNo, unlockDTO.getOrderNo())
                .eq(InventoryLockLog::getProductId, unlockDTO.getProductId())
                .eq(InventoryLockLog::getStatus, 0);
        InventoryLockLog lockLog = inventoryLockLogMapper.selectOne(
                inventoryLockLogLambdaQueryWrapper
        );
        if (lockLog == null) {
            log.warn("无有效锁定记录，无需解锁，订单号：{}", unlockDTO.getOrderNo());
            return true; // 幂等返回成功，避免重复处理
        }

        // ========== 保障2：库存校验（锁定库存 ≥ 解锁数量） ==========
        Inventory inventory = inventoryMapper.selectOne(
                lambdaQuery().eq(Inventory::getProductId, unlockDTO.getProductId())
        );
        if (inventory == null) {
            log.error("商品不存在，商品ID：{}", unlockDTO.getProductId());
            return false;
        }
        // 校验：锁定库存不能小于要解锁的数量（避免超解锁）
        if (inventory.getLockedStock() < unlockDTO.getQuantity()) {
            log.error("解锁数量超过锁定库存，商品ID：{}，锁定库存：{}，解锁数量：{}",
                    inventory.getProductId(), inventory.getLockedStock(), unlockDTO.getQuantity());
            return false;
        }

        // ========== 保障3：乐观锁解锁库存（防并发修改） ==========
        int updateResult = 0;
        int retryCount = 0;
        while (retryCount < 3 && updateResult <= 0) {
            // 乐观锁SQL：解锁库存（锁定库存-数量，可用库存+数量）
            updateResult = inventoryMapper.unlockStockWithOptimisticLock(
                    unlockDTO.getProductId(),
                    unlockDTO.getQuantity(),
                    inventory.getVersion()
            );
            retryCount++;
            // 重试前刷新库存版本号
            if (updateResult <= 0) {
                inventory = inventoryMapper.selectOne(
                        lambdaQuery().eq(Inventory::getProductId, unlockDTO.getProductId())
                );
                log.warn("解锁库存乐观锁冲突，重试第{}次，商品ID：{}", retryCount, unlockDTO.getProductId());
            }
        }
        if (updateResult <= 0) {
            log.error("解锁库存失败（乐观锁冲突），订单号：{}", unlockDTO.getOrderNo());
            return false;
        }

        // ========== 更新锁定日志状态（标记为“已释放”，幂等关键） ==========
        lockLog.setStatus(1); // 1-已释放
        lockLog.setReleaseTime(LocalDateTime.now());
        lockLog.setRemark("订单取消/创建失败，解锁库存，订单号：" + unlockDTO.getOrderNo());
        inventoryLockLogMapper.updateById(lockLog);

        // ========== 记录解锁操作日志（溯源） ==========
        InventoryOperationLog operationLog = new InventoryOperationLog();
        operationLog.setProductId(unlockDTO.getProductId());
        operationLog.setOperationType("RELEASE"); // 释放操作
        operationLog.setOrderNo(unlockDTO.getOrderNo());
        operationLog.setBeforeStock(inventory.getAvailableStock());
        operationLog.setAfterStock(inventory.getAvailableStock() + unlockDTO.getQuantity());
        operationLog.setChangeQuantity(unlockDTO.getQuantity()); // 解锁为正
        operationLog.setOperatorId(unlockDTO.getUserId());
        operationLog.setRemark("订单" + unlockDTO.getOrderNo() + "解锁库存");
        inventoryOperationLogMapper.insert(operationLog);

        log.info("解锁库存成功，订单号：{}，商品ID：{}，解锁数量：{}",
                unlockDTO.getOrderNo(), unlockDTO.getProductId(), unlockDTO.getQuantity());
        return true;
    }
}