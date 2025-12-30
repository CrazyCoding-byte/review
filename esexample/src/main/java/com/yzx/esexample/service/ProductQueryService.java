package com.yzx.esexample.service;

import com.yzx.esexample.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @className: ProductQueyService
 * @author: yzx
 * @date: 2025/9/22 21:42
 * @Version: 1.0
 * @description:
 */
public interface ProductQueryService {
    /**
     * 匹配查询：搜索商品名称或描述中包含指定关键词的商品
     *
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    Page<Product> searchByKeyword(String keyword, Pageable pageable);

    /**
     * 术语查询：精确匹配商品分类
     *
     * @param categoryId 分类ID
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    Page<Product> searchByCategoryId(Long categoryId, Pageable pageable);

    /**
     * 范围查询：查询价格在指定范围内的商品
     *
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    Page<Product> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<Product> complexBooleanQuery(Pageable pageable);

    /**
     * 前缀查询：查询商品编码以指定前缀开头的商品
     *
     * @param prefix 前缀
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    Page<Product> searchByCodePrefix(String prefix, Pageable pageable);

    /**
     * 通配符查询：查询商品名称符合通配符模式的商品
     *
     * @param pattern 通配符模式
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    Page<Product> searchByNameWildcard(String pattern, Pageable pageable);

    /**
     * 模糊查询：查询商品名称与指定关键词相似的商品
     *
     * @param keyword 关键词
     * @param fuzziness 模糊度
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    Page<Product> searchByNameFuzzy(String keyword, String fuzziness, Pageable pageable);

    /**
     * 范围查询：查询指定时间范围内创建的商品
     *
     * @param start 开始时间
     * @param end 结束时间
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    Page<Product> searchByCreateTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable);


}
