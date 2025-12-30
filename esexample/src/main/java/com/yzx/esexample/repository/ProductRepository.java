package com.yzx.esexample.repository;

import com.yzx.esexample.entity.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * @className: ProductRepository
 * @author: yzx
 * @date: 2025/9/22 20:37
 * @Version: 1.0
 * @description:
 */
@Repository
public interface ProductRepository extends ElasticsearchRepository<Product,Long> {
}
