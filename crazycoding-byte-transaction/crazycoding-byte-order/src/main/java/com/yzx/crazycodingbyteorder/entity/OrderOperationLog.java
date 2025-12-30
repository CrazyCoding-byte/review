// order/src/main/java/com/demo/order/entity/OrderOperationLog.java
package com.yzx.crazycodingbyteorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("order_operation_log")
public class OrderOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("order_no")
    private String orderNo;
    
    @TableField("operation_type")
    private String operationType;
    
    @TableField("operator_id")
    private Long operatorId;
    
    @TableField("operator_name")
    private String operatorName;
    
    @TableField("before_status")
    private Integer beforeStatus;
    
    @TableField("after_status")
    private Integer afterStatus;
    
    @TableField("remark")
    private String remark;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}