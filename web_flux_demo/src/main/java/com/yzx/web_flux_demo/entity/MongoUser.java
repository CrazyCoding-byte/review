package com.yzx.web_flux_demo.entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @className: MongoUser
 * @author: yzx
 * @date: 2025/11/27 13:45
 * @Version: 1.0
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
public class MongoUser {
    @Id
    private String id;
    private String name;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;
    private List<String> phoneNumbers; // 新增：一个用户有多个电话号码
}
