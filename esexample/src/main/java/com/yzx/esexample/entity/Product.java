package com.yzx.esexample.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @className: Product
 * @author: yzx
 * @date: 2025/9/22 19:15
 * @Version: 1.0
 * @description:
 */
@Data
@Document(indexName = "product")
public class Product {

    /**
     * 商品ID
     */
    @Id
    private Long id;

    /**
     * 商品名称，分词索引
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    /**
     * 商品编码，精确匹配
     */
    @Field(type = FieldType.Keyword)
    private String code;

    /**
     * 商品描述，分词索引
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /**
     * 商品价格
     */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /**
     * 商品分类ID
     */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /**
     * 商品分类名称
     */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /**
     * 商品标签
     */
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    /**
     * 商品销量
     */
    @Field(type = FieldType.Integer)
    private Integer sales;

    /**
     * 用户评分
     */
    @Field(type = FieldType.Float)
    private Float score;

    /**
     * 库存数量
     */
    @Field(type = FieldType.Integer)
    private Integer stock;

    /**
     * 是否上架
     */
    @Field(type = FieldType.Boolean)
    private Boolean isOnSale;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updateTime;
}
