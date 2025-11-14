package com.yzx.web_flux_demo.servlet;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @className: ServletFilter
 * @author: yzx
 * @date: 2025/9/10 17:18
 * @Version: 1.0
 * @description:
 */
@Component
public class ServletFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange);
    }
}