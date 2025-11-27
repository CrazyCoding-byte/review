package com.yzx.web_flux_demo.repository;

import com.yzx.web_flux_demo.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @className: ComplexUserService
 * @author: yzx
 * @date: 2025/11/27 18:22
 * @Version: 1.0
 * @description:
 */
@Service
@Slf4j
public class ComplexUserService {
    public static void main(String[] args) {
        Mono<Tuple3<Integer, Integer, String>> helloworld = Mono.zip(Mono.just(1), Mono.just(2), Mono.just("helloworld"));
        helloworld.subscribe(tuple -> log.info("tuple:{}", tuple));
        Map<String, List<MongoUser>> map = new HashMap<>();
        List<MongoUser> mongoUsers = new ArrayList<>();
        MongoUser yzx = new MongoUser("1", "yzx", "yzx@gmail.com", 14, null,
                Arrays.asList("13800138001", "13800138002"));
        MongoUser yzx1 = new MongoUser("2", "yzx", "yzx@gmail.com", 15, null,
                Arrays.asList("13900139001"));
        MongoUser yzx2 = new MongoUser("3", "yzx", "yzx@gmail.com", 16, null,
                Arrays.asList("13700137001", "13700137002", "13700137003"));
        mongoUsers.add(yzx);
        mongoUsers.add(yzx1);
        mongoUsers.add(yzx2);
        map.put("mongoUsers", mongoUsers);
        List<MongoUser> collect = map.get("mongoUsers").stream().sorted(Comparator.comparing(MongoUser::getAge).reversed()).collect(Collectors.toList());
        MongoUser mongoUsers1 = map.get("mongoUsers").stream().max(Comparator.comparing(MongoUser::getAge)).stream().findFirst().get();
        map.get("mongoUsers").stream().allMatch(mongoUser -> mongoUser.getAge() > 18);
        // 使用reduce方法找到年龄最大的用户
        MongoUser mongoUsers2 = map.get("mongoUsers").stream().reduce((user1, user2) -> user1.getAge() > user2.getAge() ? user1 : user2).get();
        //计算评价年龄
        double averageAge = map.get("mongoUsers").stream().map(MongoUser::getAge).reduce(0, Integer::sum) / (double) map.get("mongoUsers").size();
        List<String> phones = map.get("mongoUsers").stream().flatMap(user -> user.getPhoneNumbers().stream()).collect(Collectors.toList());
        //提取出来深层的属性
        log.info("phones:{}", phones);
        log.info("averageAge:{}", averageAge);
        log.info("mongoUsers2:{}", mongoUsers2);
        log.info("mongoUsers1:{}", mongoUsers1);
        log.info("collect:{}", collect);
    }

    private final MongoUserRepository userRepository;
    private final OrderRepository orderRepository;

    public ComplexUserService(MongoUserRepository userService, OrderRepository orderService) {
        this.userRepository = userService;
        this.orderRepository = orderService;
    }

    //条件查询+转换+错误处理
    public Flux<MongoUserDto> getAdultUserWithOrder() {
        return userRepository.findByAgeGreaterThan(18).
                //提取当前用户所有的order
                        flatMap(user -> orderRepository.findByUserId(user.getId()).
                        collectList().
                        map(orders -> new MongoUserDto(user.getId(), user.getName(), user.getEmail(), user.getAge(), orders.size())))
                .onErrorComplete(throwable -> {
                    log.error("error:{}", throwable);
                    return false;
                });
    }

    //并行处理多个数据源
    public Mono<UserDetailDto> getUserDetailWithParallelCalls(String userId) {
        Mono<MongoUser> userMono = userRepository.findById(userId);
        Flux<Order> ordersFlux = orderRepository.findByUserId(userId);
        Mono<Long> orderCountMono = orderRepository.findByUserId(userId).count();
        Mono<BigDecimal> totalAmountMono = orderRepository.findByUserId(userId)
                .map(Order::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Mono.zip(userMono, ordersFlux.collectList(), orderCountMono, totalAmountMono)
                .map(tuple -> new UserDetailDto(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()))
                .timeout(Duration.ofSeconds(5))
                .onErrorReturn(UserDetailDto.createErrorDTO());
    }
    //被压控制+批量处理
//    public Flux<User> processUsersWithBackPressure(Flux<User> users) {
//
//    }
}
