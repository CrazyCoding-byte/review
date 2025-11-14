package com.yzx.web_flux_demo.generics;

import com.yzx.web_flux_demo.entity.Author;
import com.yzx.web_flux_demo.entity.Book;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @className: DistinctByUserName
 * @author: yzx
 * @date: 2025/9/10 17:46
 * @Version: 1.0
 * @description:
 */
public class DistinctByUserName {

    public static void main(String[] args) {
        // 生成测试数据
        List<Author> authors = generateTestData();
        System.out.println(authors);
        // 使用flatMap将所有作者的书籍扁平化为一个流
        List<Book> allBooks = authors.stream()
                .flatMap(author -> author.getBooks().stream())
                .collect(Collectors.toList());
        System.out.printf("所有的book%s", allBooks);
        System.out.println("所有书籍（包含重复）：");


        // 统计每本书出现的次数（演示flatMap后的操作） flatMap扁平化数据
        authors.stream().map(new Function<Author, List<Book>>() {
            @Override
            public List<Book> apply(Author author) {
                return author.getBooks();
            }
        }).forEach(new Consumer<List<Book>>() {
            @Override
            public void accept(List<Book> books) {
                for (Book book : books) {
                    System.out.println(book);
                }
            }
        });
    }

    // 生成包含重复书籍数据的作者列表
    private static List<Author> generateTestData() {
        List<Author> authors = new ArrayList<>();
        Random random = new Random();

        // 定义可能的书籍名称（包含重复出现概率高的名称）
        List<String> bookNames = Arrays.asList(
                "Java编程思想", "Python入门到精通", "C++ Primer",
                "JavaScript高级程序设计", "Java编程思想", "Spring实战",
                "Python入门到精通", "算法导论", "Java编程思想",
                "数据结构与算法", "Python入门到精通", "设计模式"
        );

        // 生成5位作者，每位作者随机拥有3-6本书
        for (int i = 0; i < 5; i++) {
            List<Book> books = new ArrayList<>();
            // 每位作者的书籍数量随机
            int bookCount = 3 + random.nextInt(4); // 3-6本

            for (int j = 0; j < bookCount; j++) {
                // 随机选择书籍名称（会产生重复）
                String randomBookName = bookNames.get(random.nextInt(bookNames.size()));
                books.add(new Book(randomBookName));
            }

            authors.add(new Author("作者" + (i + 1), 25 + random.nextInt(20), books));
        }

        return authors;
    }

}
