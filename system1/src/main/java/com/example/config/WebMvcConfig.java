package com.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//第二步：注册拦截器（告诉 Spring 哪些接口要拦）
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有 /api 开头的请求
                .addPathPatterns("/api/**")
                // 🔥 核心修改：不要放行整个 auth，只放行登录、注册
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/captcha/**"
                );
    }
}