// wms/src/main/java/com/yzx/crazycodingbytemms/entity/InventoryLockLog.java
package com.yzx.crazycodingbytewms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存锁定记录表（对应inventory_lock_log表）
 */
@Data
@TableName("inventory_lock_log")
public class InventoryLockLog {
    @TableId(type = IdType.AUTO)
    private Long id;                // 锁定记录ID

    @TableField("lock_no")
    private String lockNo;          // 锁定流水号

    @TableField("order_no")
    private String orderNo;         // 订单号

    @TableField("product_id")
    private Long productId;         // 商品ID

    @TableField("quantity")
    private Integer quantity;       // 锁定数量

    @TableField("status")
    private Integer status;         // 0-已锁定, 1-已释放, 2-已扣减

    @TableField("lock_time")
    private LocalDateTime lockTime; // 锁定时间

    @TableField("release_time")
    private LocalDateTime releaseTime; // 释放时间

    @TableField("deduct_time")
    private LocalDateTime deductTime;   // 扣减时间

    @TableField("expire_time")
    private LocalDateTime expireTime;   // 过期时间

    @TableField("remark")
    private String remark;          // 备注

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;   // 创建时间
}