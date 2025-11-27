package com.yzx.web_flux_demo.repository;

import com.yzx.web_flux_demo.entity.Order;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/**
 * @className: OrderRepository
 * @author: yzx
 * @date: 2025/11/27 18:21
 * @Version: 1.0
 * @description:
 */
public interface OrderRepository extends ReactiveMongoRepository<Order,String> {
    Flux<Order> findByUserId(String userId);

    Flux<Order> findByStatus(String status);
}
