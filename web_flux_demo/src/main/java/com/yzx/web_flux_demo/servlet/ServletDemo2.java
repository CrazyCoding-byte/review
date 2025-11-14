package com.yzx.web_flux_demo.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * @className: ServletDemo2
 * @author: yzx
 * @date: 2025/9/10 16:59
 * @Version: 1.0
 * @description:
 */
@WebServlet(urlPatterns = "/hello1")
public class ServletDemo2 extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
         resp.sendRedirect("https://www.baidu.com");
    }
}
