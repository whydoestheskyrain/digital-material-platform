package com.trade.virtual_trade.config;
import com.trade.virtual_trade.security.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 后端上传的图片（商品+头像）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/uploads/**",
                        "/user/register", "/user/login", "/user/get/**",
                        "/product/list", "/product/detail/**", "/product/listByStatus/1",
                        "/gameType/enabled", "/gameType/detail/**",
                        "/alipay/notify", "/recharge/notify", "/recharge/redirect",
                        "/pet/chat", "/pet/chat/stream", "/pet/skin/all", "/pet/skin/detail/**",
                        "/ws/**"
                );
    }
}


