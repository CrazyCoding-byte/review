package com.yzx.web_flux_demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @className: UserDto
 * @author: yzx
 * @date: 2025/11/8 9:49
 * @Version: 1.0
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String name;
    private Long id;
}
