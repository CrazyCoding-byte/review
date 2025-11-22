package com.yzx.web_flux_demo.net.config;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 路由管理器。负责注册路由和根据请求查找匹配的路由。
 */
public class Router {
    private final Map<String, Map<Pattern, Route>> routes = new HashMap<>(); // method -> pattern -> Route

    /**
     * 注册一个路由。
     * @param method HTTP方法 (GET, POST, etc.)
     * @param path 路径模板 (如 /user/{id})
     * @param handler 该路由对应的业务逻辑处理器
     */
    public void register(String method, String path, RequestHandler handler) {
        String upperMethod = method.toUpperCase();
        Pattern pattern = pathToPattern(path);
        routes.computeIfAbsent(upperMethod, k -> new HashMap<>()).put(pattern, new Route(upperMethod, path, pattern, handler));
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
     * @param routePattern 路由的原始路径模板 (如 /user/{id})
     * @param requestPath 实际的请求路径 (如 /user/123)
     * @return 包含参数名和值的 Map
     */
    public Map<String, String> extractPathParams(String routePattern, String requestPath) {
        Map<String, String> params = new HashMap<>();
        String[] patternParts = routePattern.split("/");
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
}
