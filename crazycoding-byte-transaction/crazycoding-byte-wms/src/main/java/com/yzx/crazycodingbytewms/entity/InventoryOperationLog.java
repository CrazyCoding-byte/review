// wms/src/main/java/com/yzx/crazycodingbytemms/entity/InventoryOperationLog.java
package com.yzx.crazycodingbytewms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存操作日志表（对应inventory_operation_log表）
 */
@Data
@TableName("inventory_operation_log")
public class InventoryOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;                // 操作日志ID

    @TableField("product_id")
    private Long productId;         // 商品ID

    @TableField("operation_type")
    private String operationType;   // 操作类型: LOCK, DEDUCT, RELEASE, ADJUST

    @TableField("order_no")
    private String orderNo;         // 订单号

    @TableField("before_stock")
    private Integer beforeStock;    // 操作前库存

    @TableField("after_stock")
    private Integer afterStock;     // 操作后库存

    @TableField("change_quantity")
    private Integer changeQuantity; // 变动数量

    @TableField("operator_id")
    private Long operatorId;        // 操作人ID

    @TableField("remark")
    private String remark;          // 备注

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;   // 创建时间
}