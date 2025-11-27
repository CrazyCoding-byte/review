package com.yzx.web_flux_demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * @className: UserDetailDto
 * @author: yzx
 * @date: 2025/11/27 18:24
 * @Version: 1.0
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailDto {
    private MongoUser user;
    private List<Order> orders;
    private Long totalOrders;
    private BigDecimal totalAmount;

    public static UserDetailDto createErrorDTO() {
        return new UserDetailDto(null, Collections.emptyList(), 0L, BigDecimal.ZERO);
    }
}
