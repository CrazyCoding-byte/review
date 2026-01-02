package com.yzx.crazycodingbytepay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("local_message")
public class LocalMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("message_id")
    private String messageId;        // 消息ID（唯一）
    
    @TableField("business_type")
    private String businessType;     // 业务类型：ORDER_CREATE、ORDER_CANCEL等
    
    @TableField("business_id")
    private String businessId;       // 业务ID：订单号等
    
    @TableField("status")
    private Integer status;          // 0-待发送，1-已发送，2-发送失败
    
    @TableField("retry_count")
    private Integer retryCount;      // 重试次数
    
    @TableField("max_retry")
    private Integer maxRetry;        // 最大重试次数
    
    @TableField("payload")
    private String payload;          // 消息内容（JSON）
    
    @TableField("error_msg")
    private String errorMsg;         // 错误信息
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}