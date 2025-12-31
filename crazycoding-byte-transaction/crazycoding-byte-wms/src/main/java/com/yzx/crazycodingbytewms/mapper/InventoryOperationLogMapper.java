// wms/src/main/java/com/yzx/crazycodingbytemms/mapper/InventoryOperationLogMapper.java
package com.yzx.crazycodingbytewms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yzx.crazycodingbytewms.entity.InventoryOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存操作日志Mapper（操作inventory_operation_log表）
 */
@Mapper
public interface InventoryOperationLogMapper extends BaseMapper<InventoryOperationLog> {
}