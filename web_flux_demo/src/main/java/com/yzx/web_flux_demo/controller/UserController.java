package com.yzx.web_flux_demo.controller;

import com.yzx.web_flux_demo.entity.UserDto;
import com.yzx.web_flux_demo.repository.UserService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @className: UserController
 * @author: yzx
 * @date: 2025/11/8 10:03
 * @Version: 1.0
 * @description:
 */
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 查询
    @GetMapping("/{id}")
    public Mono<UserDto> getUser(@PathVariable Long id) {
        return userService.getUserId(id);
    }

    // 新增
    @PostMapping
    public Mono<UserDto> createUser(@RequestBody UserDto dto) {
        return userService.saveUser(dto);
    }
}
