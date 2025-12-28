package com.yzx.chatdemo.service.imple;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.util.ObjectBuilder;
import com.yzx.chatdemo.entity.Product;
import com.yzx.chatdemo.repository.ProductRepository;
import com.yzx.chatdemo.service.ProductQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.springframework.data.domain.*;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
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
        MultiMatchQueryBuilder multiMatchQuery = multiMatchQuery(keyword, "name", "description");
        NativeQuery query = new NativeQueryBuilder().withQuery((Function<Query.Builder, ObjectBuilder<Query>>) multiMatchQuery.type("best_fields") // 最佳字段匹配
                        .operator(Operator.OR) // 关键词之间是OR关系
                        .fuzziness(Fuzziness.AUTO)) // 自动模糊匹配
                .withPageable(pageable).build();
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
        TermQueryBuilder termQuery = termQuery("categoryId", categoryId);
        // 创建术语查询，精确匹配categoryId
        NativeQuery query = new NativeQueryBuilder().withQuery((Function<Query.Builder, ObjectBuilder<Query>>) termQuery).withPageable(pageable).build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);

        List<Product> products = searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());

        return new org.springframework.data.domain.PageImpl<>(products, pageable, searchHits.getTotalHits());
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
        NativeQuery query = new NativeQueryBuilder().withQuery((Function<Query.Builder, ObjectBuilder<Query>>) rangeQuery).withPageable(pageable).build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(query, Product.class);
        return null;
    }

    /**
     * 前缀查询
     * @param prefix 前缀
     * @param pageable 分页参数
     * @return
     */
    @Override
    public Page<Product> searchByCodePrefix(String prefix, Pageable pageable) {
        log.info("按照编码前缀查询商品,前缀{}", prefix);
        if (StringUtils.isEmpty(prefix)) {
            log.error("前缀不能为空");
            throw new IllegalArgumentException("前缀不能为空");
        }
        NativeQuery nativeQueryBuilder = new NativeQueryBuilder().withQuery((Function<Query.Builder, ObjectBuilder<Query>>) prefixQuery("code", prefix)).withPageable(pageable).build();
        SearchHits<Product> search = elasticsearchOperations.search(nativeQueryBuilder, Product.class);
        List<Product> collect = search.stream().map(SearchHit::getContent).collect(Collectors.toList());

        return null;
    }

    /**
     * 通配符查询:查询商品名称符合通配符模式的商品
     * @param pattern 通配符模式
     * @param pageable 分页参数
     * @return
     */
    @Override
    public Page<Product> searchByNameWildcard(String pattern, Pageable pageable) {
        log.info("按照名称通配符查询商品,模式{}", pattern);
        if (StringUtils.isEmpty(pattern)) {
            log.error("模式不能为空");
            throw new IllegalArgumentException("模式不能为空");
        }
        WildcardQueryBuilder wildcardQuery = wildcardQuery("name", pattern);
        NativeQuery nativeQueryBuilder = new NativeQueryBuilder().withQuery((Function<Query.Builder, ObjectBuilder<Query>>) wildcardQuery).withPageable(pageable).build();

        SearchHits<Product> searchHits = elasticsearchOperations.search(nativeQueryBuilder, Product.class);

        List<Product> products = searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
        return null;
    }

    /**
     * 模糊查询:查询商品名称包含指定关键词的商品
     * @param keyword 关键词
     * @param fuzziness 模糊度
     * @param pageable 分页参数
     * @return
     */
    @Override
    public Page<Product> searchByNameFuzzy(String keyword, String fuzziness, Pageable pageable) {
        log.info("按照名称模糊查询商品,关键词{},模糊度{}", keyword, fuzziness);
        if (StringUtils.isEmpty(keyword)) {
            log.error("关键词不能为空");
            throw new IllegalArgumentException("关键词不能为空");
        }
        FuzzyQueryBuilder fuzzyQuery = fuzzyQuery("name", keyword);
        //// 设置模糊度，可选值：0, 1, 2, "AUTO"
        if (StringUtils.hasText(fuzziness)) {
            fuzzyQuery.fuzziness(Fuzziness.fromString(fuzziness));
        } else {
            fuzzyQuery.fuzziness(Fuzziness.AUTO);
        }
        //设置前缀长度
        fuzzyQuery.prefixLength(1);
        NativeQuery nativeQueryBuilder = new NativeQueryBuilder().withQuery((Function<Query.Builder, ObjectBuilder<Query>>) fuzzyQuery).withPageable(pageable).build();
        SearchHits<Product> searchHits = elasticsearchOperations.search(nativeQueryBuilder, Product.class);
        List<Product> products = searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
        return null;
    }

    @Override
    public Page<Product> searchByCreateTimeRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        log.info("按照创建时间范围查询商品,开始时间: {}, 结束时间: {}", start, end);
        if (ObjectUtils.isEmpty(start) || ObjectUtils.isEmpty(end)) {
            log.error("时间范围不能为空");
            throw new IllegalArgumentException("时间范围不能为空");
        }
        RangeQueryBuilder rangeQuery = rangeQuery("createTime");
        if (!ObjectUtils.isEmpty(start)) {
            rangeQuery.gte(start); //大于等于开始时间
        }
        if (!ObjectUtils.isEmpty(end)) {
            rangeQuery.lte(end);  //小于等于结束时间
        }
        NativeQuery nativeQueryBuilder = new NativeQueryBuilder().withQuery((Function<Query.Builder, ObjectBuilder<Query>>) rangeQuery).withPageable(pageable).build();
        SearchHits<Product> searchHits = elasticsearchOperations.search(nativeQueryBuilder, Product.class);
        List<Product> products = searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
        return null;
    }

    /**
     * 查询 "价格在 500-1000 元之间、分类为智能手机、评分 4.5 以上、名称
     * 或描述包含 ' 高级 ' 或' 智能 '、且标签包含 ' 热销 ' 或' 爆款 ' 的上架商品"复杂布尔查询
     * @param pageable 分页参数
     * @return
     */
    public Page<Product> complexBooleanQuery(Pageable pageable) {
        log.info("复杂查询");
        //构建布尔查询
        BoolQueryBuilder boolQuery = boolQuery();
        //必须匹配:价格在500~1000之间
        boolQuery.filter(rangeQuery("price").gte(500).lte(1000)); //价格在 500-1000 元之间
        boolQuery.filter(termQuery("categoryName", "智能手机")); //分类为智能手机
        boolQuery.filter(rangeQuery("score").gte(4.5));
        boolQuery.filter(termQuery("isOnSale", true)); //上架商品
        //因该匹配
        BoolQueryBuilder shouldQuery = boolQuery();
        shouldQuery.should(matchQuery("name", "高级").boost(2.0f));//提升权重
        shouldQuery.should(matchQuery("name", "智能").boost(2.0f));
        shouldQuery.should(matchQuery("description", "高级").boost(1.5f));
        shouldQuery.should(matchQuery("description", "智能").boost(1.5f));
        boolQuery.minimumShouldMatch(1);//至少匹配一个
        boolQuery.must(shouldQuery);
        // 应该匹配：标签包含'热销'或'爆款'（至少满足一个）
        BoolQueryBuilder tagQuery = boolQuery();
        tagQuery.should(termQuery("tags", "热销"));
        tagQuery.should(termQuery("tags", "爆款"));
        boolQuery.should(tagQuery).boost(1.5f);
        NativeQuery nativeQueryBuilder = new NativeQueryBuilder()
                .withQuery((Function<Query.Builder, ObjectBuilder<Query>>) boolQuery)
                .withPageable(pageable).build();
        SearchHits<Product> searchHits = elasticsearchOperations.search(nativeQueryBuilder, Product.class);
        List<Product> products = searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
        return null;
    }

    public Page<Product> searchWithSortAndPage(
            String keyword,
            int pageNum,
            int pageSize,
            String sortField,
            String sortDir
    ) {
        log.info("带排序和分页的查询,关键词:{},页码:{},每页大小:{},排序字段:{},排序方向:{}", keyword, pageNum, pageSize, sortField, sortDir);
        if (!StringUtils.hasText(keyword)) {
            log.error("关键词不能为空");
            throw new IllegalArgumentException("关键词不能为空");
        }
        if (pageNum < 0) {
            pageNum = 0;
        }
        if (pageSize < 0 || pageSize > 100) {
            pageSize = 20;//默认每页20条
        }
        //验证排序字段是否合法
        List<String> validSortFields = Arrays.asList("name", "price", "score", "sales", "stock", "createTime");
        if (!StringUtils.hasText(sortField) || !validSortFields.contains(sortField)) {
            sortField = "score";//默认按评分排序
        }
        //排序方向
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageable = PageRequest.of(pageNum, pageSize, Sort.by(sortDirection, sortField));
        NativeQuery nativeQueryBuilder = new NativeQueryBuilder()
                .withQuery((Function<Query.Builder, ObjectBuilder<Query>>) multiMatchQuery(keyword, "name", "description"))
                .withPageable(pageable)
                .build();
        SearchHits<Product> searchHits = elasticsearchOperations.search(nativeQueryBuilder, Product.class);
        List<Product> products = searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
        return null;
    }

    //es聚合查询
    public Map<String, Object> aggregationByCategory() {
        log.info("按分类聚合统计商品信息");
        //1.按分类ID和分类名称进行分组
        AggregationBuilders.terms("by_category")
                .field("categoryId")
                .size(10)
                .order(BucketOrder.count(false));//按照数量降序
    }
}
