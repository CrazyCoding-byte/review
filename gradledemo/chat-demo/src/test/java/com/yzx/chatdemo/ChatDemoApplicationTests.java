package com.yzx.chatdemo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
import com.yzx.chatdemo.entity.Product;
import com.yzx.chatdemo.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;

@SpringBootTest
@Slf4j
class ChatDemoApplicationTests {

    @Autowired
    private ProductService productService;
    @Autowired
    private ElasticsearchClient esClient;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    void contextLoads() {
        long count = productService.count();
        System.out.println("商品数量：" + count);
        for (Product product : productService.findAll()) {
            System.out.println(product);
        }
        boolean b = productService.existsById(1L);
        System.out.println(b);
    }

    /**
     * 匹配查询：搜索商品名称或描述中包含指定关键词的商品
     *
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Test
    void testElasticsearch1() {
        NativeQuery build = NativeQuery.builder().
                withQuery(q -> q.multiMatch(m -> m.query("手机").
                        fields("name", "description").
                        type(TextQueryType.BestFields).operator(Operator.Or).fuzziness("AUTO")))
                .build();
        SearchHits<Product> search = elasticsearchOperations.search(build, Product.class);
        List<Product> collect = search.stream().map(SearchHit::getContent).collect(Collectors.toList());
        System.out.println(collect);
    }

    /**
     * 术语查询：精确匹配商品分类
     *
     * @param categoryId 分类ID
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Test
    void testElasticsearch2() {
        NativeQuery build = NativeQuery.builder()
                .withQuery(q -> q.term(t -> t.value(1).field("category"))).build();
        SearchHits<Product> search = elasticsearchOperations.search(build, Product.class);
        List<Product> collect = search.stream().map(SearchHit::getContent).collect(Collectors.toList());
        System.out.println(collect);
    }

    /**
     * 范围查询：查询价格在指定范围内的商品
     *
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Test
    void testElasticsearch7() {
        NativeQuery build = NativeQuery.builder()
                .withQuery(q -> q.range(r ->
                        r.field("price")       // 指定字段是price
                                .gte(JsonData.of(BigDecimal.valueOf(500)))  // 价格 ≥ 500
                                .lte(JsonData.of(BigDecimal.valueOf(500))) // 价格 ≤ 1000
                )).build();
        SearchHits<Product> search = elasticsearchOperations.search(build, Product.class);
        List<Product> collect = search.stream().map(SearchHit::getContent).collect(Collectors.toList());
        System.out.println(collect);
    }

    /**
     * 模糊查询：查询商品名称与指定关键词相似的商品
     *
     * @param keyword 关键词
     * @param fuzziness 模糊度
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Test
    void testElasticsearch4() {
        NativeQuery build = NativeQuery.builder()
                .withQuery(q -> q.fuzzy(f -> f.field("code").value("1001")))
                .build();
        SearchHits<Product> search = elasticsearchOperations.search(build, Product.class);
        List<Product> collect = search.stream().map(SearchHit::getContent).collect(Collectors.toList());
        System.out.println(collect);
    }

    /**
     * 通配符查询：查询商品名称符合通配符模式的商品
     *
     * @param pattern 通配符模式
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Test
    void testElasticsearch5() {
        NativeQuery build = NativeQuery.builder()
                .withQuery(q -> q.wildcard(w -> w.field("name").value("*手机*")))
                .build();
        SearchHits<Product> search = elasticsearchOperations.search(build, Product.class);
        List<Product> collect = search.stream().map(SearchHit::getContent).collect(Collectors.toList());
        System.out.println(collect);
    }

    /**
     * 前缀查询：查询商品编码以指定前缀开头的商品
     *
     * @param prefix 前缀
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Test
    void testElasticsearch6() {
        NativeQuery build = NativeQuery.builder()
                .withQuery(q -> q.prefix(p -> p.field("code").value("1001")))
                .build();
        SearchHits<Product> search = elasticsearchOperations.search(build, Product.class);
        List<Product> collect = search.stream().map(SearchHit::getContent).collect(Collectors.toList());
        System.out.println(collect);
    }

    /**
     * 复杂布尔查询示例
     *
     * @param pageable 分页参数
     * @return 商品分页列表
     */
    @Test
    void testElasticsearch8() {
        NativeQuery build = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b.
                                //必须过滤
                                        filter(f -> f.
                                        range(r -> r.
                                                field("price").
                                                gte(JsonData.of(500)).
                                                lte(JsonData.of(1000))))
                                .filter(f -> f.term(t -> t.field("categoryId").value(1)))
                                .filter(f -> f.range(r -> r.field("score").gte(JsonData.of(4.5))))
                                .filter(f -> f.term(t -> t.field("isOnSale").value(true)))
                                //必须匹配其中一个
                                .must(m -> m.bool(shouldbool -> {
                                    shouldbool.should(s -> s.match(match -> match.field("name").query("高级").boost(2.0f)));
                                    shouldbool.should(s -> s.match(match -> match.field("name").query("智能").boost(2.0f)));
                                    shouldbool.should(s -> s.match(match -> match.field("description").query("高级")));
                                    shouldbool.should(s -> s.match(match -> match.field("description").query("智能")));
                                    shouldbool.minimumShouldMatch("1");
                                    return shouldbool;
                                }))
                                .should(s -> s.bool(shouldbool -> {
                                    shouldbool.should(s1 -> s1.term(match -> match.field("tags").value("热销")));
                                    shouldbool.should(s1 -> s1.term(match -> match.field("tags").value("爆款")));
//                            shouldbool.boost(1.5f);  注意这里这样写无效
                                    shouldbool.minimumShouldMatch("1");
                                    return shouldbool;
                                })).boost(1.5f)
                )).build();
        SearchHits<Product> search = elasticsearchOperations.search(build, Product.class);
        List<Product> collect = search.stream().map(SearchHit::getContent).collect(Collectors.toList());
        System.out.println(collect);
    }

    /**
     * 带排序和分页的查询
     *
     * @param keyword 关键词
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param sortField 排序字段
     * @param sortDir 排序方向（asc/desc）
     * @return 商品分页列表
     */
    @Test
    void testElasticsearch9(String keyword,
                            int page,
                            int size,
                            String sortField,
                            String sortDir) {
        log.info("带排序和分页的查询，关键词: {}, 页码: {}, 每页大小: {}, 排序字段: {}, 排序方向: {}",
                keyword, page, size, sortField, sortDir);

        if (!StringUtils.hasText(keyword)) {
            log.error("关键词不能为空");
            throw new IllegalArgumentException("关键词不能为空");
        }

        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20; // 限制每页大小，防止过大
        }

        // 验证排序字段是否合法
        List<String> validSortFields = Arrays.asList(
                "price", "sales", "score", "createTime", "updateTime"
        );
        if (!StringUtils.hasText(sortField) || !validSortFields.contains(sortField)) {
            sortField = "score"; // 默认按评分排序
        }
        // 排序方向
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

        // 创建分页和排序参数
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortField)
        );
        NativeQuery build = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(m -> m.fields("name", "description").query(keyword)))
                .withPageable(pageable)
                .build();
        SearchHits<Product> searchHits = elasticsearchOperations.search(build, Product.class);

        List<Product> products = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
        PageImpl<Product> products1 = new PageImpl<>(
                products,
                pageable,
                searchHits.getTotalHits();

    }

    /**
     * 按分类聚合统计商品信息
     *
     * @return 聚合统计结果
     */
    @Test
    void testElasticsearch11() {
        log.info("按分类聚合统计商品信息（ES 8.x版本）");
        AverageAggregation build = AggregationBuilders.avg().build();
        // 1. 构建聚合（一级聚合+子聚合）
        // 一级聚合：按categoryId分组
        Aggregation categoryAgg = Aggregation.of(a -> a
                .terms(t -> t
                        .field("categoryId")
                        .size(10)
                )
                // 子聚合必须添加在 terms 聚合之后，作为整体返回对象的 aggregations
                .aggregations("category_name", Aggregation.of(sub -> sub
                        .terms(st -> st.field("categoryName").size(1))
                ))
                .aggregations("avg_price", Aggregation.of(sub -> sub
                        .avg(avg -> avg.field("price"))
                ))
                .aggregations("max_score", Aggregation.of(sub -> sub
                        .max(max -> max.field("score"))
                ))
                .aggregations("total_sales", Aggregation.of(sub -> sub
                        .sum(sum -> sum.field("sales"))
                ))
        );

        // 2. 构建NativeQuery（核心：用withAggregation替代withAggregations）
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m)) // 匹配所有文档
                .withAggregation("by_category", categoryAgg) // 单数withAggregation，参数：聚合名+聚合对象
                .withPageable(PageRequest.of(0, 0)) // 不返回文档，只取聚合结果
                .build();

        // ====================== 3. 执行查询 ======================
        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);

        // ====================== 4. 解析聚合结果（完全对齐你给的示例） ======================
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> categoryStats = new ArrayList<>();

        // 检查是否有聚合结果
        if (searchHits.hasAggregations()) {
            // ① 强转聚合容器（解决爆红核心步骤）
            ElasticsearchAggregations aggregationsContainer =
                    (ElasticsearchAggregations) searchHits.getAggregations();

            // ② 获取指定名称的聚合包装对象
            ElasticsearchAggregation categoryAggWrapper = aggregationsContainer.get("by_category");

            if (categoryAggWrapper != null) {
                // ③ 关键：从包装对象获取底层Aggregate
                Aggregate categoryAggregate = categoryAggWrapper.aggregation().getAggregate();

                // ④ 判断是否为字符串词条聚合（StringTerms）
                if (categoryAggregate.isSterms()) {
                    StringTermsAggregate sterms = categoryAggregate.sterms();

                    // ⑤ 遍历每个分类桶
                    for (StringTermsBucket bucket : sterms.buckets().array()) {
                        Map<String, Object> categoryStat = new HashMap<>();

                        // 分类ID（StringTermsBucket的key用stringValue()）
                        categoryStat.put("categoryId", bucket.key().stringValue());
                        // 商品数量
                        categoryStat.put("productCount", bucket.docCount());

                        // ====================== 解析子聚合 ======================
                        // 获取桶内的所有子聚合Map
                        Map<String, Aggregate> bucketAggs = bucket.aggregations();

                        // 子聚合1：分类名称
                        Aggregate nameAgg = bucketAggs.get("category_name");
                        if (nameAgg != null) {
                            Boolean isSterms = nameAgg.isSterms();
                            if (Boolean.TRUE.equals(isSterms)) {
                                StringTermsAggregate nameTerms = nameAgg.sterms();
                                if (nameTerms != null && nameTerms.buckets() != null) {
                                    // 直接用List接收桶，避免数组类型问题
                                    List<StringTermsBucket> buckets = nameTerms.buckets().array();
                                    // 判空
                                    if (!CollectionUtils.isEmpty(buckets)) {
                                        categoryStat.put("categoryName", buckets.get(0).key().stringValue());
                                    }
                                }
                            }
                        }

                        // 子聚合2：平均价格
                        Aggregate avgPriceAgg = bucketAggs.get("avg_price");
                        double avgPrice = avgPriceAgg != null ? avgPriceAgg.avg().value() : 0.0;
                        categoryStat.put("avgPrice", avgPrice);

                        // 子聚合3：最高评分
                        Aggregate maxScoreAgg = bucketAggs.get("max_score");
                        double maxScore = maxScoreAgg != null ? maxScoreAgg.max().value() : 0.0;
                        categoryStat.put("maxScore", maxScore);

                        // 子聚合4：总销量
                        Aggregate totalSalesAgg = bucketAggs.get("total_sales");
                        double totalSales = totalSalesAgg != null ? totalSalesAgg.sum().value() : 0.0;
                        categoryStat.put("totalSales", totalSales);

                        categoryStats.add(categoryStat);
                    }
                }
            }
        }

        // 封装最终结果
        result.put("categoryStats", categoryStats);
        result.put("totalCategories", categoryStats.size());

        // 打印结果
        System.out.println("聚合统计结果：" + result);
    }

    /**
     * 高亮查询：高亮显示匹配的关键词
     *
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 带高亮信息的商品分页列表
     */
    @Test
    void testElasticsearch10() {

    }

    @Test
    void testElasticsearch() throws IOException {
        SearchResponse<Product> response = esClient.search(s -> s
                        .index("product")
                        .query(q -> q
                                .match(t -> t
                                        .field("name")
                                        .query("手机")
                                )
                        ),
                Product.class
        );
        response.aggregations().forEach((k, v) -> {
            System.out.println("key:" + k);
            System.out.println("value:" + v);
        });
        System.out.println("我被打印了");
    }


    @Test
    void testElasticsearch3() throws IOException {
        // 1. 构建查询（与之前相同）
        Aggregation categoryAggregation = new Aggregation.Builder()
                .terms(t -> t.field("category.keyword"))
                .aggregations("avg_amount",
                        new Aggregation.Builder().avg(a -> a.field("amount")).build())
                .aggregations("order_count",
                        new Aggregation.Builder().valueCount(vc -> vc.field("orderId")).build())
                .build();

        NativeQuery query = NativeQuery.builder()
                .withAggregation("category_agg", categoryAggregation)
                .withMaxResults(0)
                .build();

        // 2. 执行查询
        var searchHits = elasticsearchOperations.search(query, Order.class);

        // 3. ✅ 正确的解析方式
        if (searchHits.hasAggregations()) {
            // 获取聚合容器
            ElasticsearchAggregations aggregationsContainer =
                    (ElasticsearchAggregations) searchHits.getAggregations();

            // 获取指定名称的聚合包装对象
            ElasticsearchAggregation categoryAggWrapper =
                    aggregationsContainer.get("category_agg"); // 使用 get() 方法

            if (categoryAggWrapper != null) {
                // ✅ 关键：从包装对象中获取底层的 Aggregate
                Aggregate categoryAggregate = categoryAggWrapper.aggregation().getAggregate();

                // 判断并处理词条聚合
                if (categoryAggregate.isSterms()) {
                    StringTermsAggregate sterms = categoryAggregate.sterms();

                    // 遍历桶
                    for (StringTermsBucket bucket : sterms.buckets().array()) {
                        String category = bucket.key().stringValue();
                        long docCount = bucket.docCount();

                        // ✅ 正确获取子聚合：先从桶的聚合Map中获取包装对象，再取底层Aggregate
                        Map<String, Aggregate> bucketAggs = bucket.aggregations();

                        // 解析平均金额
                        Aggregate avgAmountAgg = bucketAggs.get("avg_amount");
                        double avgAmount = avgAmountAgg != null ? avgAmountAgg.avg().value() : 0.0;

                        // 解析订单计数
                        Aggregate countAgg = bucketAggs.get("order_count");
                        long orderCount = countAgg != null ? (long) countAgg.valueCount().value() : 0;

                        System.out.printf("类别: %s, 文档数: %d, 平均金额: %.2f, 订单数: %d%n",
                                category, docCount, avgAmount, orderCount);
                    }
                }
            }
        }
    }
}
