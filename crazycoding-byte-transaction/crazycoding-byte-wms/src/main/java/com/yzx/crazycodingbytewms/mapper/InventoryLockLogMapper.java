// wms/src/main/java/com/yzx/crazycodingbytemms/mapper/InventoryLockLogMapper.java
package com.yzx.crazycodingbytewms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yzx.crazycodingbytewms.entity.InventoryLockLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存锁定日志Mapper（操作inventory_lock_log表）
 */
@Mapper
public interface InventoryLockLogMapper extends BaseMapper<InventoryLockLog> {

    /**
     * 根据订单号查询未过期的锁定记录
     */
    @Select("SELECT * FROM inventory_lock_log " +
            "WHERE order_no = #{orderNo} " +
            "AND status = 0 " +
            "AND expire_time > NOW()")
    InventoryLockLog selectValidLockByOrderNo(@Param("orderNo") String orderNo);
}