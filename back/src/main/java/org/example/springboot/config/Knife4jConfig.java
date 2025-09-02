package org.example.springboot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Knife4j API文档配置类
 * 用于配置Knife4j的API文档展示及相关资源访问
 * Knife4j是一个基于Swagger的API文档增强工具
 */
@Configuration
public class Knife4jConfig implements WebMvcConfigurer {
    /**
     * 配置Knife4j的静态资源路径
     * 将/api/**的请求映射到Knife4j的静态资源目录
     * 确保Knife4j的Web界面资源能够被正确访问
     *
     * @param registry ResourceHandlerRegistry对象，用于注册资源处理器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/**")

                .addResourceLocations("classpath:/META-INF/resources/")
                .resourceChain(false);
    }

    /**
     * 配置OpenAPI对象
     * 用于生成API文档的核心配置
     * 包含API文档的基本信息、安全配置等
     *
     * @return OpenAPI 返回配置好的OpenAPI对象
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // 配置接口文档基本信息
                .info(this.getApiInfo())
                ;
    }

    /**
     * 获取API文档的基本信息配置
     * 配置API文档的标题、描述、版本等元数据信息
     * 支持以下配置项：
     * - 文档标题
     * - 文档描述
     * - 作者信息（当前已注释）
     * - 许可证信息（当前已注释）
     * - 服务条款（当前已注释）
     * - 版本信息
     *
     * @return Info 返回API文档基本信息对象
     */
    private Info getApiInfo() {
        return new Info()
                // 配置文档标题
                .title("SpringBoot3文档")
                // 配置文档描述
                .description("SpringBoot3集成Knife4j文档")
                // 配置作者信息
//                .contact(new Contact().name("hello").url("https://www.baidu.cn").email("1234@163.com"))
                // 配置License许可证信息
//                .license(new License().name("Apache 2.0").url("https://www.baidu.cn"))
                // 概述信息
                .summary("SpringBoot3集成Knife4j文档")
//                .termsOfService("https://www.baidu.cn")
                // 配置版本号
                .version("2.0");
    }
}
