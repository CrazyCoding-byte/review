package com.yzx.web_flux_demo.repository;

import com.yzx.web_flux_demo.entity.User;
import com.yzx.web_flux_demo.entity.UserDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.ExecutorService;

/**
 * @className: UserService
 * @author: yzx
 * @date: 2025/11/8 9:27
 * @Version: 1.0
 * @description:
 */
// 服务层
@Service
public class UserService {
    private final UserRepository userRepository;
    private final Scheduler jdbcExecutor; // 还是需要线程池隔离阻塞操作

    public UserService(UserRepository userRepository,
                       Scheduler jdbcExecutor) {
        this.userRepository = userRepository;
        this.jdbcExecutor = jdbcExecutor;
    }

    //获取用户id
    public Mono<UserDto> getUserId(Long id) {
        return Mono.fromSupplier(() -> userRepository.
                        findById(id).
                        orElseThrow(() -> new RuntimeException("用户不存在"))).subscribeOn(jdbcExecutor)
                .map(user -> covertDto(user));
    }

    //保存用户
    public Mono<UserDto> saveUser(UserDto dto) {
        return Mono.fromSupplier(() -> {
                    User user = new User();
                    user.setId(dto.getId());
                    user.setName(dto.getName());
                    return userRepository.save(user);
                }).subscribeOn(jdbcExecutor)
                .map(user -> covertDto(user));
    }

    private UserDto covertDto(User user) {
        return new UserDto(user.getName(), user.getId());
    }
}