// wms/src/main/java/com/yzx/crazycodingbytemms/entity/Inventory.java
package com.yzx.crazycodingbytewms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存表（对应inventory表）
 */
@Data
@TableName("inventory")
public class Inventory {
    @TableId(type = IdType.AUTO)
    private Long id;                  // 库存ID

    @TableField("product_id")
    private Long productId;           // 商品ID

    @TableField("product_name")
    private String productName;       // 商品名称

    @TableField("total_stock")
    private Integer totalStock;       // 总库存

    @TableField("available_stock")
    private Integer availableStock;   // 可用库存

    @TableField("locked_stock")
    private Integer lockedStock;      // 锁定库存

    @TableField("version")
    @Version // MyBatisPlus乐观锁注解
    private Integer version;          // 版本号（乐观锁）

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime; // 更新时间
}