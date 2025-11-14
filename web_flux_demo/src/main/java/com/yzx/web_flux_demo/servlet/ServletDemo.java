package com.yzx.web_flux_demo.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * @className: ServletDemo
 * @author: yzx
 * @date: 2025/9/10 16:27
 * @Version: 1.0
 * @description:
 */
@WebServlet(urlPatterns = "/hello")
public class ServletDemo extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String method = req.getMethod();
        Map<String, String[]> parameterMap = req.getParameterMap();
        String[] usernames = parameterMap.get("username");
        String username = usernames[0];
        System.out.printf("用户名:%s", username);
        String[] passwords = parameterMap.get("password");
        String password = passwords[0];
        System.out.printf("密码:%s", password);
        System.out.printf("方法:%s", method);
        // 设置响应类型:
        resp.setContentType("text/html");
        // 获取输出流:
        PrintWriter pw = resp.getWriter();
        // 写入响应:
        pw.write("<h1>Hello, world!</h1>");
        // 最后不要忘记flush强制输出:
        pw.flush();
    }
}
