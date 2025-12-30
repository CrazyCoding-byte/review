//package com.yzx.chatdemo.service.imple;
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.elasticsearch._types.SortOrder;
//import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
//import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
//import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
//import co.elastic.clients.elasticsearch._types.aggregations.TermsOrder;
//import co.elastic.clients.elasticsearch._types.query_dsl.*;
//import co.elastic.clients.elasticsearch.core.SearchRequest;
//import co.elastic.clients.elasticsearch.core.SearchResponse;
//import co.elastic.clients.json.JsonData;
//import co.elastic.clients.util.NamedValue;
//import com.yzx.chatdemo.entity.Product;
//import com.yzx.chatdemo.repository.ProductRepository;
//import com.yzx.chatdemo.service.ProductQueryService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.*;
//import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
//import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
//import org.springframework.data.elasticsearch.client.elc.NativeQuery;
//import org.springframework.data.elasticsearch.core.AggregationsContainer;
//import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
//import org.springframework.data.elasticsearch.core.SearchHit;
//import org.springframework.data.elasticsearch.core.SearchHits;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ProductQueyServiceImpl implements ProductQueryService {
//    private final ElasticsearchOperations elasticsearchOperations;
//    private final ProductRepository productRepository;
//    private final ElasticsearchClient elasticsearchClient;
//
//    /**
//     * 匹配查询：搜索商品名称或描述中包含指定关键词的商品
//     */
//    @Override
//    public Page<Product> searchByKeyword(String keyword, Pageable pageable) {
//        log.info("分页查询所有商品，页码: {}, 每页大小: {}", pageable.getPageNumber(), pageable.getPageSize());
//
//        if (!StringUtils.hasText(keyword)) {
//            log.error("关键词不能为空");
//            throw new IllegalArgumentException("关键词不能为空");
//        }
//
//        // 正确的新版 API 写法
//        NativeQuery query = NativeQuery.builder()
//                .withQuery(q -> q
//                        .multiMatch(m -> m
//                                .query(keyword)
//                                .fields("name", "description")
//                                .type(TextQueryType.BestFields)
//                                .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
//                                .fuzziness(co.elastic.clients.elasticsearch._types.query_dsl.Fuzziness.Auto)
//                        )
//                )
//                .withPageable(pageable)
//                .build();
//
//        SearchHits<Product> search = elasticsearchOperations.search(query, Product.class);
//        List<Product> collect = search.stream()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(collect, pageable, search.getTotalHits());
//    }
//
//    /**
//     * 术语查询：精确匹配商品分类
//     */
//    @Override
//    public Page<Product> searchByCategoryId(Long categoryId, Pageable pageable) {
//        log.info("按分类查询商品，分类ID: {}", categoryId);
//
//        if (categoryId == null) {
//            log.error("分类ID不能为空");
//            throw new IllegalArgumentException("分类ID不能为空");
//        }
//
//        NativeQuery query = NativeQuery.builder()
//                .withQuery(q -> q
//                        .term(t -> t
//                                .field("categoryId")
//                                .value(categoryId)
//                        )
//                )
//                .withPageable(pageable)
//                .build();
//
//        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);
//        List<Product> products = searchHits.stream()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(products, pageable, searchHits.getTotalHits());
//    }
//
//    /**
//     * 范围查询：查询价格在指定范围内的商品
//     */
//    @Override
//    public Page<Product> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
//        log.info("按价格范围查询商品，最低价格: {}, 最高价格: {}", minPrice, maxPrice);
//
//        NativeQuery query = NativeQuery.builder()
//                .withQuery(q -> q
//                        .range(r -> {
//                            r.field("price");
//                            if (minPrice != null) {
//                                r.gte(JsonData.of(minPrice.doubleValue()));
//                            }
//                            if (maxPrice != null) {
//                                r.lte(JsonData.of(maxPrice.doubleValue()));
//                            }
//                            return r;
//                        })
//                )
//                .withPageable(pageable)
//                .build();
//
//        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);
//        List<Product> products = searchHits.stream()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(products, pageable, searchHits.getTotalHits());
//    }
//
//    /**
//     * 聚合查询 - 修正版本
//     */
//    public Map<String, Object> aggregationByCategory() throws IOException {
//        // 修正后的完整聚合请求（仅改order部分，其他保留）
//        SearchRequest searchRequest = SearchRequest.of(s -> s
//                .index("product") // 你的商品索引名
//                .size(0) // 不返回文档，只查聚合
//                // 主聚合：按分类ID分组
//                .aggregations("by_category", a -> a
//                        .terms(t -> t
//                                .field("categoryId") // 聚合字段：分类ID
//                                .size(10) // 最多返回10个分类
//                                // 【核心修正】order方法传 NamedValue<SortOrder> 类型参数
//                                // 对应 IDE 提示的 “NamedValue<SortOrder> value” 重载
//                                .order(NamedValue.of("_count", SortOrder.Desc))
//                                // 子聚合1：分类名称（取第一个）
//                                .a
//                                // 子聚合2：平均价格
//                                .aggregations("avg_price", subAgg -> subAgg
//                                        .avg(avg -> avg.field("price"))
//                                )
//                                // 子聚合3：最高评分
//                                .aggregations("max_score", subAgg -> subAgg
//                                        .max(max -> max.field("score"))
//                                )
//                                // 子聚合4：总销量（已修正拼写错误）
//                                .aggregations("total_sales", subAgg -> subAgg
//                                        .sum(sum -> sum.field("sales"))
//                                )
//                        )
//                )
//        );
//
//        SearchResponse<Product> search = elasticsearchClient.search(searchRequest, Product.class);
//        List<StringTermsBucket> CountList = search.aggregations()
//                .get("author_count")
//                .sterms()
//                .buckets()
//                .array();
//        System.out.println(CountList);
//
//    }
//
//    /**
//     * 复杂布尔查询 - 修正版本
//     */
//    @Override
//    public Page<Product> complexBooleanQuery(Pageable pageable) {
//        log.info("复杂查询");
//
//        NativeQuery query = NativeQuery.builder()
//                .withQuery(q -> q
//                        .bool(b -> {
//                            // filter 条件
//                            b.filter(f -> f
//                                    .range(r -> r
//                                            .field("price")
//                                            .gte(JsonData.of(500))
//                                            .lte(JsonData.of(1000))
//                                    )
//                            );
//                            b.filter(f -> f
//                                    .term(t -> t
//                                            .field("categoryName")
//                                            .value("智能手机")
//                                    )
//                            );
//                            b.filter(f -> f
//                                    .range(r -> r
//                                            .field("score")
//                                            .gte(JsonData.of(4.5))
//                                    )
//                            );
//                            b.filter(f -> f
//                                    .term(t -> t
//                                            .field("isOnSale")
//                                            .value(true)
//                                    )
//                            );
//
//                            // must 条件
//                            b.must(m -> m
//                                    .bool(b2 -> {
//                                        b2.should(s -> s
//                                                .match(m2 -> m2
//                                                        .field("name")
//                                                        .query("高级")
//                                                        .boost(2.0f)
//                                                )
//                                        );
//                                        b2.should(s -> s
//                                                .match(m2 -> m2
//                                                        .field("name")
//                                                        .query("智能")
//                                                        .boost(2.0f)
//                                                )
//                                        );
//                                        b2.should(s -> s
//                                                .match(m2 -> m2
//                                                        .field("description")
//                                                        .query("高级")
//                                                        .boost(1.5f)
//                                                )
//                                        );
//                                        b2.should(s -> s
//                                                .match(m2 -> m2
//                                                        .field("description")
//                                                        .query("智能")
//                                                        .boost(1.5f)
//                                                )
//                                        );
//                                        b2.minimumShouldMatch("1");
//                                        return b2;
//                                    })
//                            );
//
//                            // should 条件
//                            b.should(s -> s
//                                    .bool(b2 -> {
//                                        b2.should(s2 -> s2
//                                                .term(t -> t
//                                                        .field("tags")
//                                                        .value("热销")
//                                                )
//                                        );
//                                        b2.should(s2 -> s2
//                                                .term(t -> t
//                                                        .field("tags")
//                                                        .value("爆款")
//                                                )
//                                        );
//                                        return b2;
//                                    })
//                            ).boost(1.5f);
//
//                            return b;
//                        })
//                )
//                .withPageable(pageable)
//                .build();
//
//        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);
//        List<Product> products = searchHits.stream()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(products, pageable, searchHits.getTotalHits());
//    }
//
//    /**
//     * 前缀查询 - 修正版本
//     */
//    @Override
//    public Page<Product> searchByCodePrefix(String prefix, Pageable pageable) {
//        log.info("按照编码前缀查询商品,前缀{}", prefix);
//
//        if (!StringUtils.hasText(prefix)) {
//            log.error("前缀不能为空");
//            throw new IllegalArgumentException("前缀不能为空");
//        }
//
//        NativeQuery query = NativeQuery.builder()
//                .withQuery(q -> q
//                        .prefix(p -> p
//                                .field("code")
//                                .value(prefix)
//                        )
//                )
//                .withPageable(pageable)
//                .build();
//
//        SearchHits<Product> search = elasticsearchOperations.search(query, Product.class);
//        List<Product> collect = search.stream()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(collect, pageable, search.getTotalHits());
//    }
//
//    /**
//     * 通配符查询 - 修正版本
//     */
//    @Override
//    public Page<Product> searchByNameWildcard(String pattern, Pageable pageable) {
//        log.info("按照名称通配符查询商品,模式{}", pattern);
//
//        if (!StringUtils.hasText(pattern)) {
//            log.error("模式不能为空");
//            throw new IllegalArgumentException("模式不能为空");
//        }
//
//        NativeQuery query = NativeQuery.builder()
//                .withQuery(q -> q
//                        .wildcard(w -> w
//                                .field("name")
//                                .value(pattern)
//                        )
//                )
//                .withPageable(pageable)
//                .build();
//
//        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);
//        List<Product> products = searchHits.stream()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(products, pageable, searchHits.getTotalHits());
//    }
//
//    /**
//     * 模糊查询 - 修正版本
//     */
//    @Override
//    public Page<Product> searchByNameFuzzy(String keyword, String fuzziness, Pageable pageable) {
//        log.info("按照名称模糊查询商品,关键词{},模糊度{}", keyword, fuzziness);
//
//        if (!StringUtils.hasText(keyword)) {
//            log.error("关键词不能为空");
//            throw new IllegalArgumentException("关键词不能为空");
//        }
//
//        NativeQuery query = NativeQuery.builder()
//                .withQuery(q -> q
//                        .fuzzy(f -> {
//                            f.field("name")
//                                    .value(keyword)
//                                    .fuzziness(StringUtils.hasText(fuzziness) ?
//                                            co.elastic.clients.elasticsearch._types.query_dsl.Fuzziness.of(f2 -> f2.string(fuzziness)) :
//                                            co.elastic.clients.elasticsearch._types.query_dsl.Fuzziness.Auto)
//                                    .prefixLength(1);
//                            return f;
//                        })
//                )
//                .withPageable(pageable)
//                .build();
//
//        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);
//        List<Product> products = searchHits.stream()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(products, pageable, searchHits.getTotalHits());
//    }
//
//    /**
//     * 按创建时间范围查询 - 修正版本
//     */
//    @Override
//    public Page<Product> searchByCreateTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
//        log.info("按照创建时间范围查询商品,开始时间: {}, 结束时间: {}", start, end);
//
//        if (start == null || end == null) {
//            log.error("时间范围不能为空");
//            throw new IllegalArgumentException("时间范围不能为空");
//        }
//
//        NativeQuery query = NativeQuery.builder()
//                .withQuery(q -> q
//                        .range(r -> {
//                            r.field("createTime");
//                            if (start != null) {
//                                r.gte(JsonData.of(start.toString()));
//                            }
//                            if (end != null) {
//                                r.lte(JsonData.of(end.toString()));
//                            }
//                            return r;
//                        })
//                )
//                .withPageable(pageable)
//                .build();
//
//        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);
//        List<Product> products = searchHits.stream()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(products, pageable, searchHits.getTotalHits());
//    }
//}