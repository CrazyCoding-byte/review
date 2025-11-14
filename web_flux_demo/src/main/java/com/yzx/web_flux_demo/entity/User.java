package com.yzx.web_flux_demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @className: User
 * @author: yzx
 * @date: 2025/11/8 9:16
 * @Version: 1.0
 * @description:
 */
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    private Long id;
    private String name;
}
