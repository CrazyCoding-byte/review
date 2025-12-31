// order/src/main/java/com/yzx/crazycodingbyteorder/dto/InventoryLockDTO.java
package com.yzx.crazycodingbytewms.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 库存锁定DTO（订单服务 ↔ 库存服务 通信用）
 */
@Data
public class InventoryLockDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderNo;        // 订单号
    private Long productId;        // 商品ID
    private Integer quantity;      // 锁定/扣减/解锁数量
    private Long userId;           // 用户ID
    private String lockNo;         // 锁定流水号（库存服务生成）
    private Integer operateType;   // 操作类型：1-锁库存 2-解锁库存 3-扣减库存
}