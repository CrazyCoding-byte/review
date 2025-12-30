// order/src/main/java/com/demo/order/vo/OrderVO.java
package com.yzx.crazycodingbyteorder.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String orderNo;
    private Long userId;
    private Integer status;
    private String statusDesc;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    
    private List<OrderDetailVO> orderDetails;
    
    @Data
    public static class OrderDetailVO implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long productId;
        private String productName;
        private BigDecimal productPrice;
        private Integer quantity;
        private BigDecimal totalPrice;
        private String productImage;
        private String productSpec;
    }
}