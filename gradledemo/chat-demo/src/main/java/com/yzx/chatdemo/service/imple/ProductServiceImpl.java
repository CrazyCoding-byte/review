package com.yzx.chatdemo.service.imple;

import com.yzx.chatdemo.entity.Product;
import com.yzx.chatdemo.repository.ProductRepository;
import com.yzx.chatdemo.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Optional;

/**
 * @className: ProductServiceImpl
 * @author: yzx
 * @date: 2025/9/22 19:44
 * @Version: 1.0
 * @description:
 */

/**
 * 商品服务实现类
 *
 * @author ken
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    /**
     * 创建或更新商品
     *
     * @param product 商品信息
     * @return 保存后的商品信息
     */
    @Override
    public Product save(Product product) {
        log.info("保存商品: {}", product.getId());

        if (ObjectUtils.isEmpty(product)) {
            log.error("商品信息不能为空");
            throw new IllegalArgumentException("商品信息不能为空");
        }

        return productRepository.save(product);
    }

    /**
     * 批量创建或更新商品
     *
     * @param products 商品列表
     * @return 保存后的商品列表
     */
    @Override
    public Iterable<Product> saveAll(List<Product> products) {
        log.info("批量保存商品，数量: {}", products.size());

        if (ObjectUtils.isEmpty(products)) {
            log.error("商品列表不能为空");
            throw new IllegalArgumentException("商品列表不能为空");
        }

        return productRepository.saveAll(products);
    }

    /**
     * 根据ID查询商品
     *
     * @param id 商品ID
     * @return 商品信息，不存在则返回空
     */
    @Override
    public Optional<Product> findById(Long id) {
        log.info("查询商品，ID: {}", id);

        if (ObjectUtils.isEmpty(id)) {
            log.error("商品ID不能为空");
            return Optional.empty();
        }

        return productRepository.findById(id);
    }

    /**
     * 查询所有商品
     *
     * @return 商品列表
     */
    @Override
    public Iterable<Product> findAll() {
        log.info("查询所有商品");
        return productRepository.findAll();
    }

    /**
     * 分页查询所有商品
     *
     * @param pageable 分页参数
     * @return 分页商品列表
     */
    @Override
    public Page<Product> findAll(Pageable pageable) {
        log.info("分页查询所有商品，页码: {}, 每页大小: {}", pageable.getPageNumber(), pageable.getPageSize());
        return productRepository.findAll(pageable);
    }

    /**
     * 根据ID删除商品
     *
     * @param id 商品ID
     */
    @Override
    public void deleteById(Long id) {
        log.info("删除商品，ID: {}", id);

        if (ObjectUtils.isEmpty(id)) {
            log.error("商品ID不能为空");
            throw new IllegalArgumentException("商品ID不能为空");
        }

        productRepository.deleteById(id);
    }

    /**
     * 检查商品是否存在
     *
     * @param id 商品ID
     * @return 是否存在
     */
    @Override
    public boolean existsById(Long id) {
        log.info("检查商品是否存在，ID: {}", id);

        if (ObjectUtils.isEmpty(id)) {
            log.error("商品ID不能为空");
            return false;
        }

        return productRepository.existsById(id);
    }

    /**
     * 查询商品总数
     *
     * @return 商品总数
     */
    @Override
    public long count() {
        log.info("查询商品总数");
        return productRepository.count();
    }
}

