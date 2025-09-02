package org.example.springboot.config;

import org.example.springboot.util.FileUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MyInterceptorConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
                Path projectRootPath = null;
                projectRootPath = Paths.get(FileUtil.FILE_BASE_PATH);
                String imgFolderPath = projectRootPath.resolve("img").toAbsolutePath().toString();

                // 添加两个资源映射，一个带/api前缀，一个不带
                registry.addResourceHandler("/api/img/**").addResourceLocations("file:" + imgFolderPath + "/");
                registry.addResourceHandler("/img/**").addResourceLocations("file:" + imgFolderPath + "/");

                registry.addResourceHandler("doc.html")
                                .addResourceLocations("classpath:/META-INF/resources/");
                registry.addResourceHandler("swagger-ui.html")
                                .addResourceLocations("classpath:/META-INF/resources/");
                registry.addResourceHandler("/webjars/**")
                                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        }
}
