package com.yzx.web_flux_demo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @className: Author
 * @author: yzx
 * @date: 2025/9/10 17:50
 * @Version: 1.0
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Author implements Comparable<Author> {
    private String name;
    private Integer age;
    private List<Book> books;

    @Override
    public int compareTo(Author o) {
        return this.age - o.age;
    }
}
