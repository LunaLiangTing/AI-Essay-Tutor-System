package com.example.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Info;
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI(){
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot 3 API接口文档")
                        .version("1.0.0")
                        .description("本文档涵盖了所有模块的接口说明")
                        .contact(new Contact().name("婷子工程师").url("https://javaxmsz.cn")));
    }
}
