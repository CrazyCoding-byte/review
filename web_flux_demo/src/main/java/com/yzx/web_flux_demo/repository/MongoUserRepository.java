package com.yzx.web_flux_demo.repository;

import com.yzx.web_flux_demo.entity.MongoUser;
import com.yzx.web_flux_demo.entity.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @className: MongoUserRepository
 * @author: yzx
 * @date: 2025/11/27 18:20
 * @Version: 1.0
 * @description:
 */
public interface MongoUserRepository extends ReactiveMongoRepository<MongoUser, String> {
    Flux<MongoUser> findByAgeGreaterThan(int age);
    Mono<MongoUser> findByEmail(String email);
}
