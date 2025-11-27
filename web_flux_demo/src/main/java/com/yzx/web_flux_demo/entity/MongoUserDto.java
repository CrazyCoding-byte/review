package com.yzx.web_flux_demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @className: MongoUserDto
 * @author: yzx
 * @date: 2025/11/27 18:23
 * @Version: 1.0
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MongoUserDto {
    private String id;
    private String name;
    private String email;
    private Integer age;
    private Integer orderCount;
}
