// wms/src/main/java/com/yzx/crazycodingbytemms/service/InventoryService.java
package com.yzx.crazycodingbytewms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yzx.crazycodingbytewms.dto.InventoryLockDTO;
import com.yzx.crazycodingbytewms.entity.Inventory;

/**
 * 库存Service（核心锁/解锁/扣减逻辑）
 */
public interface InventoryService extends IService<Inventory> {

    /**
     * 锁库存（核心方法：基于乐观锁+行锁保证原子性）
     * @param lockDTO 订单传递的锁定参数
     * @return true=锁定成功，false=锁定失败
     */
    boolean lockStock(InventoryLockDTO lockDTO);

    /**
     * 解锁库存（核心方法：基于乐观锁+行锁保证原子性）
     * @param lockDTO 订单传递的解锁参数
     * @return true=解锁成功，false=解锁失败
     */
    boolean unlockStock(InventoryLockDTO unlockDTO);
}