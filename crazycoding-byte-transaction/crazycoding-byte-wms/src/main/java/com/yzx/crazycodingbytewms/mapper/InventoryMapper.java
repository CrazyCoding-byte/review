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
     * 乐观锁锁定库存（无available_stock字段版）
     * 核心修改：
     * 1. 移除available_stock更新
     * 2. 用 (total_stock - locked_stock) >= #{quantity} 判断可用库存是否足够
     * 3. 仅更新locked_stock和version
     */
    @Update("UPDATE inventory " +
            "SET locked_stock = locked_stock + #{quantity}, " +  // 仅增加锁定库存
            "version = version + 1, " +
            "update_time = NOW() " +
            "WHERE product_id = #{productId} " +
            "AND (total_stock - locked_stock) >= #{quantity} " + // 实时计算可用库存并校验
            "AND version = #{version}")
    int lockStockWithOptimisticLock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity,
            @Param("version") Integer version
    );

    /**
     * 根据商品ID查询库存（加行锁，防止并发）
     * 核心修改：
     * 1. 移除available_stock字段查询
     * 2. 新增 (total_stock - locked_stock) AS available_stock，保持返回值兼容业务代码
     */
    @Select("SELECT id, product_id, product_name, total_stock, locked_stock, " +
            "(total_stock - locked_stock) AS available_stock, " + // 实时计算可用库存，兼容业务层取值
            "version, create_time, update_time " +
            "FROM inventory WHERE product_id = #{productId} FOR UPDATE")
    Inventory selectByProductIdForUpdate(@Param("productId") Long productId);

    /**
     * 乐观锁解锁库存（无available_stock字段版）
     * 核心修改：
     * 1. 移除available_stock更新
     * 2. 仅更新locked_stock和version
     */
    @Update("UPDATE inventory " +
            "SET locked_stock = locked_stock - #{quantity}, " +  // 仅减少锁定库存
            "version = version + 1, " +
            "update_time = NOW() " +
            "WHERE product_id = #{productId} " +
            "AND locked_stock >= #{quantity} " + // 确保锁定库存足够解锁
            "AND version = #{version}")
    int unlockStockWithOptimisticLock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity,
            @Param("version") Integer version
    );

}