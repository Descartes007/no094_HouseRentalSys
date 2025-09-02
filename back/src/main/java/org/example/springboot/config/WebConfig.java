package org.example.springboot.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类，用于自定义Spring MVC的行为
 * 主要功能：
 * 1. 配置全局API路径前缀
 * 2. 配置JWT拦截器及其路径规则
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String API_PREFIX = "/api";
    
    // 定义不需要JWT验证的路径
    private static final String[] PUBLIC_PATHS = {
        "/api/user/login",      // 登录接口
        "/api/user/forget",     // 忘记密码接口
        "/api/user/add",        // 用户注册接口
        "/api/user/{id}",       // 用户信息查询接口
        "/api/email/**",        // 邮件相关接口
        "/api/img/**",          // 图片资源接口（带前缀）
        "/img/**",              // 图片资源接口（不带前缀）
        "/api/file/**",         // 文件资源接口（带前缀）
        "/file/**",             // 文件资源接口（不带前缀）
        
        // Swagger和API文档相关路径
        "/api/v3/api-docs/**",
        "/api/swagger-ui.html",
        "/api/swagger-ui/**",
        "/api/doc.html",
        "/api/webjars/**",
        "/api/favicon.ico"
    };

    @Resource
    private JwtInterceptor jwtInterceptor;



    /**
     * 配置路径匹配规则
     * 为所有带有@RestController注解的控制器类添加统一的路径前缀
     * 这样可以将API接口与其他Web资源区分开
     *
     * @param configurer 路径匹配配置器
     */

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 为带有RestController注解的类添加"/api"路径前缀
        // 排除 Knife4j/Swagger 相关的接口（通过包名判断）
        configurer.addPathPrefix("/api", clazz ->
                clazz.isAnnotationPresent(RestController.class) &&
                        !clazz.getPackage().getName().contains("springfox") &&
                        !clazz.getPackage().getName().contains("swagger")&&!clazz.getPackage().getName().contains("doc")
        );
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 配置JWT拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns(API_PREFIX + "/**")    // 拦截所有API请求
                .excludePathPatterns("/api/**")
                .excludePathPatterns(PUBLIC_PATHS);     // 排除公开接口
    }
}
