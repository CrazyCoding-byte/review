// order/src/main/java/com/demo/order/entity/Order.java
package com.yzx.crazycodingbyteorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("`order`")
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("order_no")
    private String orderNo;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("product_id")
    private Long productId;
    
    @TableField("quantity")
    private Integer quantity;
    
    @TableField("total_amount")
    private BigDecimal totalAmount;
    
    @TableField("status")
    private Integer status;
    
    @TableField("pay_time")
    private LocalDateTime payTime;
    
    @TableField("cancel_time")
    private LocalDateTime cancelTime;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}