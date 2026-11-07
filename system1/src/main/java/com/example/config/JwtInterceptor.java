package com.example.config;

import com.example.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
//在之前的 StudentController 里，为了测试方便，直接传 @RequestParam Long studentId。这就相当于任何人只要传个假的学生 ID，就能查到别人的作文，这是极度不安全的！
//第一步：编写真正的 JWT 拦截器
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 放行浏览器的预检请求 (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 从 Header 中获取 Token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"401\",\"msg\":\"未登录或 Token 已过期，请重新登录\"}");
            return false;
        }

        try {
            // 3. 解析 Token
            Claims claims = jwtUtil.parseToken(token.substring(7));
            Long userId = Long.valueOf(claims.getSubject());
            String role = (String) claims.get("role");

            // 4. 将解析出的用户信息存入 request 中，方便后续 Controller 直接获取
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            return true; // 放行
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"401\",\"msg\":\"Token 无效或已过期\"}");
            return false;
        }
    }
}