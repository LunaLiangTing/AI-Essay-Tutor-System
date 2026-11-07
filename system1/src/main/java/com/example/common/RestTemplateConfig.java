package com.example.common;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration // 这是一个配置类
public class RestTemplateConfig {

    @Bean // 把这个 RestTemplate 交给 Spring 容器管理
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // 连接超时：10秒。如果网络不好，超过10秒没连上 DeepSeek 服务器，就报错
                .setConnectTimeout(Duration.ofSeconds(10))
                // 读取超时：120秒。因为 AI 生成一篇长作文需要时间，给足2分钟
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }
}
