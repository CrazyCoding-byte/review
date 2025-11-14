package com.yzx.crazycodingbytegateway.config;

import lombok.Data;

import java.util.List;

/**
 * @className: RouteConfig
 * @author: yzx
 * @date: 2025/11/14 1:18
 * @Version: 1.0
 * @description:
 */
@Data
public class RouteConfig {
    private String id;
    private String path;  //匹配路径 /user/**
    private String targetUrl;  //目标服务 http://localhost:8080
    private List<String> filters; //过滤器
    private LoadBalanceStrategy lb; //负载均衡策略

}
