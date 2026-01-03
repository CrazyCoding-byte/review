// wms/src/main/java/com/yzx/crazycodingbytewms/dto/InventoryLockResultDTO.java
package com.yzx.crazycodingbyteorder.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 库存锁定结果DTO（用于通知订单服务）
 */
@Data
public class InventoryLockResultDTO implements Serializable {
    private String orderNo;        // 订单号
    private Long productId;        // 商品ID
    private Long userId;           // 用户ID
    private boolean lockSuccess;   // 锁定是否成功
    private String failReason;     // 失败原因（锁定失败时非空）
    private LocalDateTime lockTime;// 锁定时间
}