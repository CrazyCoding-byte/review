package com.yzx.esexample.service.imple;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.util.ObjectBuilder;
import com.yzx.esexample.entity.Product;
import com.yzx.esexample.repository.ProductRepository;
import com.yzx.esexample.service.ProductQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.FuzzyQueryBuilder;
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
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductQueyServiceImpl implements ProductQueryService {
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductRepository productRepository;
    private final ElasticsearchClient elasticsearchClient;

    /**
     * 匹配查询：搜索商品名称或描述中包含指定关键词的商品
     *
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Override
    public Page<Product> searchByKeyword(String keyword, Pageable pageable) {
        log.info("搜索商品，关键词: {}", keyword);

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

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);

        // 转换为Page对象
        List<Product> products = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(
                products,
                pageable,
                searchHits.getTotalHits()
        );
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
                .withQuery(termQuery("categoryId", categoryId))
                .withPageable(pageable)
                .build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);

        List<Product> products = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                products,
                pageable,
                searchHits.getTotalHits()
        );
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

        if (ObjectUtils.isEmpty(minPrice) && ObjectUtils.isEmpty(maxPrice)) {
            log.error("最低价格和最高价格不能同时为空");
            throw new IllegalArgumentException("最低价格和最高价格不能同时为空");
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
                .withQuery(rangeQuery)
                .withPageable(pageable)
                .build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);

        List<Product> products = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                products,
                pageable,
                searchHits.getTotalHits()
        );
    }

    @Override
    public Page<Product> complexBooleanQuery(Pageable pageable) {
        return null;
    }

    /**
     * 前缀查询：查询商品编码以指定前缀开头的商品
     *
     * @param prefix 前缀
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Override
    public Page<Product> searchByCodePrefix(String prefix, Pageable pageable) {
        log.info("按编码前缀查询商品，前缀: {}", prefix);

        if (!StringUtils.hasText(prefix)) {
            log.error("前缀不能为空");
            throw new IllegalArgumentException("前缀不能为空");
        }

        // 创建前缀查询
        NativeQuery query = new NativeQueryBuilder()
                .withQuery(prefixQuery("code", prefix))
                .withPageable(pageable)
                .build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);

        List<Product> products = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                products,
                pageable,
                searchHits.getTotalHits()
        );
    }

    /**
     * 通配符查询：查询商品名称符合通配符模式的商品
     *
     * @param pattern 通配符模式
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Override
    public Page<Product> searchByNameWildcard(String pattern, Pageable pageable) {
        log.info("按名称通配符查询商品，模式: {}", pattern);

        if (!StringUtils.hasText(pattern)) {
            log.error("通配符模式不能为空");
            throw new IllegalArgumentException("通配符模式不能为空");
        }

        // 创建通配符查询
        // ? 匹配任意单个字符
        // * 匹配零个或多个字符
        NativeQuery query = new NativeQueryBuilder()
                .withQuery(wildcardQuery("name", pattern))
                .withPageable(pageable)
                .build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);

        List<Product> products = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                products,
                pageable,
                searchHits.getTotalHits()
        );
    }

    /**
     * 模糊查询：查询商品名称与指定关键词相似的商品
     *
     * @param keyword 关键词
     * @param fuzziness 模糊度
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Override
    public Page<Product> searchByNameFuzzy(String keyword, String fuzziness, Pageable pageable) {
        log.info("按名称模糊查询商品，关键词: {}, 模糊度: {}", keyword, fuzziness);

        if (!StringUtils.hasText(keyword)) {
            log.error("关键词不能为空");
            throw new IllegalArgumentException("关键词不能为空");
        }

        // 创建模糊查询
        FuzzyQueryBuilder fuzzyQuery = fuzzyQuery("name", keyword);

        // 设置模糊度，可选值：0, 1, 2, "AUTO"
        if (StringUtils.hasText(fuzziness)) {
            fuzzyQuery.fuzziness(Fuzziness.parseCustomAuto(fuzziness));
        } else {
            fuzzyQuery.fuzziness(Fuzziness.parseCustomAuto("AUTO")); // 默认自动模糊度
        }

        // 设置前缀长度，前n个字符必须精确匹配
        fuzzyQuery.prefixLength(1);

        NativeQuery query = new NativeQueryBuilder()
                .withQuery(fuzzyQuery)
                .withPageable(pageable)
                .build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);

        List<Product> products = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                products,
                pageable,
                searchHits.getTotalHits()
        );
    }

    /**
     * 范围查询：查询指定时间范围内创建的商品
     *
     * @param start 开始时间
     * @param end 结束时间
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Override
    public Page<Product> searchByCreateTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        log.info("按创建时间范围查询商品，开始时间: {}, 结束时间: {}", start, end);

        if (ObjectUtils.isEmpty(start) && ObjectUtils.isEmpty(end)) {
            log.error("开始时间和结束时间不能同时为空");
            throw new IllegalArgumentException("开始时间和结束时间不能同时为空");
        }

        // 创建时间范围查询
        RangeQueryBuilder rangeQuery = rangeQuery("createTime");

        if (!ObjectUtils.isEmpty(start)) {
            rangeQuery.gte(start); // 大于等于开始时间
        }
        if (!ObjectUtils.isEmpty(end)) {
            rangeQuery.lte(end); // 小于等于结束时间
        }

        NativeQuery query = new NativeQueryBuilder()
                .withQuery(rangeQuery)
                .withPageable(pageable)
                .build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);

        List<Product> products = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(
                products,
                pageable,
                searchHits.getTotalHits()
        );
    }


}