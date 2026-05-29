package com.zjhl.project.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession();
        Object loginUser = session.getAttribute("loginUser");

        // ======================
        // 已登录 → 直接放行
        // ======================
        if (loginUser != null) {
            return true;
        }

        // ======================
        // 未登录 → 跳登录页
        // ======================
        response.sendRedirect("/login.html");
        return false;
    }
}