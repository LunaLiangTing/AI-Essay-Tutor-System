package com.example.common;


import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsFilterConfig {
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许任何请求头
        config.addAllowedHeader("*");
        // 允许任何请求方法
        config.addAllowedMethod("*");
        // 允许携带 cookie 等凭证（必须用 setAllowedOriginPatterns）
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        // 暴露响应头（如果需要前端读取特定 header）
        config.addExposedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        // 设置最高优先级
        bean.setOrder(-100);
        return bean;
    }
}
