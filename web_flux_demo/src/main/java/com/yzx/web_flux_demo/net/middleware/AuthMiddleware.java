package com.yzx.web_flux_demo.net.middleware;

import com.yzx.web_flux_demo.net.config.core.Context;
import com.yzx.web_flux_demo.net.config.core.Request;
import com.yzx.web_flux_demo.net.config.core.Response;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import java.util.Collections;

/**
 * @className: AuthMiddleware
 * @author: yzx
 * @date: 2025/11/23 1:59
 * @Version: 1.0
 * @description:
 */
public class AuthMiddleware implements Middleware{
    // --- 依赖注入的 Spring Security 组件 ---
    // 这里需要根据您的具体配置选择使用 JWT 还是不透明令牌
    // 1. JWT 方式
    private final JwtDecoder jwtDecoder; // 通过构造函数注入
    // private final JwtAuthenticationProvider jwtAuthProvider; // 可选，如果需要更复杂的认证逻辑

    // 2. 不透明令牌方式 (需要配置 IntrospectionEndpoint)
    // private final OpaqueTokenIntrospector introspector; // 通过构造函数注入
    // private final OpaqueTokenAuthenticationProvider opaqueAuthProvider; // 可选

    // 3. Token 解析器 (可选，也可以手动解析)
    private final BearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();

    // 构造函数，注入必要的 Spring Security 组件 (这里以 JWT 为例)
    public AuthMiddleware(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
        // this.introspector = introspector; // 如果使用不透明令牌
    }

    @Override
    public void handle(Context context) throws Exception {
        Request request = context.request();
        Response response = context.response(); // 假设 config.Response 有 setHeader 方法

        try {
            // 1. 从请求头中提取 Bearer Token
            String authorizationHeader = request.header(HttpHeaderNames.AUTHORIZATION.toString());
            String token = null;
            if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer ")) {
                token = authorizationHeader.substring(7).trim(); // 去掉 "Bearer " 前缀
            }
            // 或者使用 Spring 的 BearerTokenResolver (如果注入了)
            // String token = bearerTokenResolver.resolve(request); // 这需要将 Request 适配为 ServerHttpRequest

            if (token == null || token.isEmpty()) {
                // 没有找到 Token，返回 401
                sendUnauthorizedResponse(response, context, "Missing or invalid Authorization header");
                return; // 重要：中断链执行
            }

            // 2. 验证 Token (这里以 JWT 为例)
            // Spring Security 的 JwtDecoder 会解析并验证 JWT 的签名、过期时间等
            org.springframework.security.oauth2.jwt.Jwt decodedJwt = jwtDecoder.decode(token);

            // 3. 创建 Spring Security Authentication 对象 (示例：JwtAuthenticationToken)
            // 这个对象包含了用户信息和权限
            // 注意：JwtAuthenticationToken 的构造函数可能需要额外的参数，如 JwtGrantedAuthoritiesConverter
            // 这里简化处理，直接使用 decodedJwt 的主体作为 principal
            OAuth2AuthenticatedPrincipal principal = new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                    Collections.emptyList(), // authorities, 可以从 JWT 中解析
                    decodedJwt.getClaims(),
                    "sub" // nameAttributeKey
            );
            Authentication authentication = new JwtAuthenticationToken((Jwt) principal, Collections.emptyList()); // authorities 可以从 principal 获取或通过转换器生成

            // 4. 将认证信息存入 Context，供后续处理器使用
            context.set("authentication", authentication);
            context.set("principal", principal);
            // 也可以存入用户ID等常用信息
            context.set("userId", decodedJwt.getSubject());
            // 如果需要角色信息
            // Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            // context.set("roles", authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

            // 5. Token 有效，继续执行链
            context.next();

        } catch (org.springframework.security.oauth2.jwt.JwtException e) {
            // JWT 解码或验证失败
            System.err.println("JWT validation failed: " + e.getMessage());
            sendUnauthorizedResponse(response, context, "Invalid JWT token: " + e.getMessage());
        } catch (Exception e) { // 捕获其他可能的异常
            System.err.println("Authentication failed: " + e.getMessage());
            e.printStackTrace();
            sendUnauthorizedResponse(response, context, "Authentication error: " + e.getMessage());
        }
    }

    private void sendUnauthorizedResponse(Response response, Context context, String message) {
        response.status(HttpResponseStatus.UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer realm=\"OAuth2 Resource Server\""); // 可选，提供认证方式信息
        context.text(message); // 使用 Context 的便捷方法设置响应体
        // 注意：这里调用 context.text() 可能会直接发送响应，
        // 如果没有，需要在 Http1RequestHandler/Http2RequestHandler 中检查 Context 状态
        // 或者在这里直接操作 Netty 的 ChannelHandlerContext (需要从 Context 获取)
        // 为了框架一致性，最好在 Context 中处理发送逻辑，或者设置一个标志位
        // 让 RequestHandler 检查到错误后发送。
        // 这里假设 context.text() 会触发发送。
    }
}
