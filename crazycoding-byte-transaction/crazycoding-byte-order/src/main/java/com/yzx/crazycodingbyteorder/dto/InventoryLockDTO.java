// order/src/main/java/com/demo/order/dto/InventoryLockDTO.java
package com.yzx.crazycodingbyteorder.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class InventoryLockDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String orderNo;
    private Long productId;
    private Integer quantity;
    private Long userId;
}