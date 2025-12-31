// wms/src/main/java/com/yzx/crazycodingbytemms/mapper/InventoryMapper.java
package com.yzx.crazycodingbytewms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yzx.crazycodingbytewms.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 库存Mapper（操作inventory表）
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 乐观锁锁定库存：扣减可用库存 + 增加锁定库存
     * @param productId 商品ID
     * @param quantity 锁定数量
     * @param version 版本号（乐观锁）
     * @return 影响行数（1=成功，0=失败/乐观锁冲突）
     */
    @Update("UPDATE inventory " +
            "SET available_stock = available_stock - #{quantity}, " +
            "locked_stock = locked_stock + #{quantity}, " +
            "version = version + 1 " +
            "WHERE product_id = #{productId} " +
            "AND available_stock >= #{quantity} " +
            "AND version = #{version}")
    int lockStockWithOptimisticLock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity,
            @Param("version") Integer version
    );

    /**
     * 根据商品ID查询库存（加行锁，防止并发）
     */
    @Select("SELECT * FROM inventory WHERE product_id = #{productId} FOR UPDATE")
    Inventory selectByProductIdForUpdate(@Param("productId") Long productId);

    /**
     * 乐观锁解锁库存：锁定库存-数量，可用库存+数量
     */
    @Update("UPDATE inventory " +
            "SET locked_stock = locked_stock - #{quantity}, " +
            "available_stock = available_stock + #{quantity}, " +
            "version = version + 1 " +
            "WHERE product_id = #{productId} " +
            "AND locked_stock >= #{quantity} " + // 数据库层面校验，避免负数
            "AND version = #{version}")
    int unlockStockWithOptimisticLock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity,
            @Param("version") Integer version
    );
}