package com.yzx.esexample.service;

/**
 * @className: ProductService
 * @author: yzx
 * @date: 2025/9/22 19:42
 * @Version: 1.0
 * @description:
 */

import com.yzx.esexample.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 商品服务接口
 *
 * @author ken
 */
public interface ProductService {

    /**
     * 创建或更新商品
     *
     * @param product 商品信息
     * @return 保存后的商品信息
     */
    Product save(Product product);

    /**
     * 批量创建或更新商品
     *
     * @param products 商品列表
     * @return 保存后的商品列表
     */
    Iterable<Product> saveAll(List<Product> products);

    /**
     * 根据ID查询商品
     *
     * @param id 商品ID
     * @return 商品信息，不存在则返回空
     */
    Optional<Product> findById(Long id);

    /**
     * 查询所有商品
     *
     * @return 商品列表
     */
    Iterable<Product> findAll();

    /**
     * 分页查询所有商品
     *
     * @param pageable 分页参数
     * @return 分页商品列表
     */
    Page<Product> findAll(Pageable pageable);

    /**
     * 根据ID删除商品
     *
     * @param id 商品ID
     */
    void deleteById(Long id);

    /**
     * 检查商品是否存在
     *
     * @param id 商品ID
     * @return 是否存在
     */
    boolean existsById(Long id);

    /**
     * 查询商品总数
     *
     * @return 商品总数
     */
    long count();
}
