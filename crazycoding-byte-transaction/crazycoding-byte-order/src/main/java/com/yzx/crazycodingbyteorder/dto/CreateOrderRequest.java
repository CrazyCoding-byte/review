// order/src/main/java/com/demo/order/dto/CreateOrderRequest.java
package com.yzx.crazycodingbyteorder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CreateOrderRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;
    @NotNull(message = "商品单价不能为空")
    private BigDecimal productPrice;
    private String productName;
    private String productImage;
    private String productSpec;
}