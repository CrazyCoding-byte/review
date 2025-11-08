package com.yzx.web_flux_demo.functiondemo;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;

import java.io.IOException;

/**
 * @className: ServletFilter
 * @author: yzx
 * @date: 2025/9/10 17:18
 * @Version: 1.0
 * @description:
 */
@WebFilter("/*")
public class ServletFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
