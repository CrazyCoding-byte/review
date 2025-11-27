package com.yzx.crazycodingbytehttp.config;


import com.yzx.crazycodingbytehttp.config.core.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路由管理器。负责注册路由和根据请求查找匹配的路由。
 */
public class Router {
    // 修改内部存储结构：method -> pattern -> Route
    // 现在 Route 包含了中间件列表
    private final Map<String, Map<Pattern, Route>> routes = new HashMap<>();

    /**
     * 注册一个路由 (旧方法，用于直接注册，无中间件)。
     * @param method HTTP方法 (GET, POST, etc.)
     * @param path 路径模板 (如 /user/{id})
     * @param handler 该路由对应的业务逻辑处理器
     */
    public void register(String method, String path, RequestHandler handler) {
        register(method, path, handler, null); // 调用新方法，传入空中间件列表
    }

    /**
     * 注册一个路由 (新方法，支持中间件)。
     * @param method HTTP方法 (GET, POST, etc.)
     * @param path 路径模板 (如 /user/{id})
     * @param handler 该路由对应的业务逻辑处理器
     * @param middlewares 该路由对应的中间件列表 (可为 null)
     */
    public void register(String method, String path, RequestHandler handler, List<Handler> middlewares) {
        String upperMethod = method.toUpperCase();
        Pattern pattern = pathToPattern(path);
        // 使用新的 Route 构造函数，传入中间件列表
        Route route = new Route(upperMethod, path, pattern, handler, middlewares);
        routes.computeIfAbsent(upperMethod, k -> new HashMap<>()).put(pattern, route);
    }

    /**
     * 根据请求方法和路径查找匹配的路由。
     * @param requestMethod 请求方法
     * @param requestPath 请求路径
     * @return 匹配到的 Route 对象，如果没有匹配则返回 null
     */
    public Route match(String requestMethod, String requestPath) {
        Map<Pattern, Route> pathRoutes = routes.get(requestMethod.toUpperCase());
        if (pathRoutes == null) {
            return null;
        }

        for (Map.Entry<Pattern, Route> entry : pathRoutes.entrySet()) {
            Matcher matcher = entry.getKey().matcher(requestPath);
            if (matcher.matches()) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 将路径模板转换为正则表达式 Pattern。
     * 例如，/user/{id} -> ^/user/([^/]+)$
     */
    private Pattern pathToPattern(String path) {
        String regex = path.replaceAll("\\{([^}]+)\\}", "([^/]+)");
        return Pattern.compile("^" + regex + "$");
    }

    /**
     * 从请求路径中提取路径参数。
     * @param routePath 路由的原始路径模板 (如 /user/{id})
     * @param requestPath 实际的请求路径 (如 /user/123)
     * @return 包含参数名和值的 Map
     */
    public Map<String, String> extractPathParams(String routePath, String requestPath) {
        // ... (保持原有逻辑不变)
        Map<String, String> params = new HashMap<>();
        String[] patternParts = routePath.split("/");
        String[] pathParts = requestPath.split("/");

        for (int i = 0; i < patternParts.length && i < pathParts.length; i++) {
            String part = patternParts[i];
            if (part.startsWith("{") && part.endsWith("}")) {
                String paramName = part.substring(1, part.length() - 1);
                params.put(paramName, pathParts[i]);
            }
        }
        return params;
    }

    /**
     * 创建一个路由分组。
     * @param prefix 分组的公共前缀
     * @return Group 实例
     */
    public Group Group(String prefix) {
        // 创建一个空的中间件列表，用于新分组
        return new Group(prefix, new ArrayList<>(), this);
    }

    /**
     * 创建一个带中间件的路由分组。
     * @param prefix 分组的公共前缀
     * @param middlewares 分组的公共中间件列表
     * @return Group 实例
     */
    public Group Group(String prefix, List<Handler> middlewares) {
        return new Group(prefix, middlewares, this);
    }
}
