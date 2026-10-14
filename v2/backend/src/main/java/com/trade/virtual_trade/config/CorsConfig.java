package com.trade.virtual_trade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置类，解决Vue前端调用后端接口的跨域问题
 * 允许来源通过配置项 app.cors-origins 注入（逗号分隔），生产域名不硬编码在代码中
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors-origins:http://localhost:8082}")
    private String corsOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 对所有接口路径生效
        registry.addMapping("/**")
                // 允许来自配置的前端地址（多个来源用逗号分隔，默认包含开发端口）
                .allowedOrigins(corsOrigins.split("\\s*,\\s*"))
                // 允许携带Cookie（会话信息）
                .allowCredentials(true)
                // 允许所有请求方法（GET/POST/PUT/DELETE等）
                .allowedMethods("*")
                // 允许所有请求头
                .allowedHeaders("*");
    }
}
