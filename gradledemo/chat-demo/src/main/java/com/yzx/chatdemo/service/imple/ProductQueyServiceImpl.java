package com.yzx.chatdemo.service.imple;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.util.ObjectBuilder;
import com.yzx.chatdemo.entity.Product;
import com.yzx.chatdemo.repository.ProductRepository;
import com.yzx.chatdemo.service.ProductQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * @className: ProductQueyServiceImpl
 * @author: yzx
 * @date: 2025/9/22 21:42
 * @Version: 1.0
 * @description:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductQueyServiceImpl implements ProductQueryService {
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductRepository productRepository;

    /**
     * 匹配查询：搜索商品名称或描述中包含指定关键词的商品
     *
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Override
    public Page<Product> searchByKeyword(String keyword, Pageable pageable) {
        log.info("分页查询所有商品，页码: {}, 每页大小: {}", pageable.getPageNumber(), pageable.getPageSize());
        if (!StringUtils.hasText(keyword)) {
            log.error("关键词不能为空");
            throw new IllegalArgumentException("关键词不能为空");
        }
        // 创建匹配查询，同时搜索name和description字段
        NativeQuery query = new NativeQueryBuilder()
                .withQuery((Function<Query.Builder, ObjectBuilder<Query>>) multiMatchQuery(keyword, "name", "description")
                        .type("best_fields") // 最佳字段匹配
                        .operator(Operator.OR) // 关键词之间是OR关系
                        .fuzziness(Fuzziness.AUTO)) // 自动模糊匹配
                .withPageable(pageable)
                .build();
        SearchHits<Product> search = elasticsearchOperations.search(query, Product.class);
        List<Product> collect = search.stream().map(SearchHit::getContent).collect(Collectors.toList());
        PageImpl<Product> products = new PageImpl<>(collect, pageable, search.getTotalHits());
        return products;
    }
    /**
     * 术语查询：精确匹配商品分类
     *
     * @param categoryId 分类ID
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Override
    public Page<Product> searchByCategoryId(Long categoryId, Pageable pageable) {
        log.info("按分类查询商品，分类ID: {}", categoryId);

        if (ObjectUtils.isEmpty(categoryId)) {
            log.error("分类ID不能为空");
            throw new IllegalArgumentException("分类ID不能为空");
        }

        // 创建术语查询，精确匹配categoryId
        NativeQuery query = new NativeQueryBuilder()
                .withQuery((Function<Query.Builder, ObjectBuilder<Query>>) termQuery("categoryId", categoryId))
                .withPageable(pageable)
                .build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);

        List<Product> products = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                products,
                pageable,
                searchHits.getTotalHits());
    }
    /**
     * 范围查询：查询价格在指定范围内的商品
     *
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param pageable 分页参数
     * @return 商品分页列表
     */

    @Override
    public Page<Product> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
             log.info("按价格范围查询商品，最低价格: {}, 最高价格: {}", minPrice, maxPrice);
        if (ObjectUtils.isEmpty(minPrice) || ObjectUtils.isEmpty(maxPrice)) {
            log.error("价格范围不能为空");
            throw new IllegalArgumentException("价格范围不能为空");
        }
        // 创建范围查询
        RangeQueryBuilder rangeQuery = rangeQuery("price");

        if (!ObjectUtils.isEmpty(minPrice)) {
            rangeQuery.gte(minPrice); // 大于等于最低价格
        }
        if (!ObjectUtils.isEmpty(maxPrice)) {
            rangeQuery.lte(maxPrice); // 小于等于最高价格
        }
        NativeQuery query = new NativeQueryBuilder()
                .withQuery((Function<Query.Builder, ObjectBuilder<Query>>) rangeQuery)
                .withPageable(pageable)
                .build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);
        return null;
    }

    @Override
    public Page<Product> searchByCodePrefix(String prefix, Pageable pageable) {
        return null;
    }

    @Override
    public Page<Product> searchByNameWildcard(String pattern, Pageable pageable) {
        return null;
    }

    @Override
    public Page<Product> searchByNameFuzzy(String keyword, String fuzziness, Pageable pageable) {
        return null;
    }

    @Override
    public Page<Product> searchByCreateTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return null;
    }
}
