package com.campusplant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 本地开发：映射 /uploads/ 到文件系统，使上传的图片可通过 HTTP 访问。
 * 生产环境不加载此配置（图片走 OSS CDN）。
 */
@Configuration
@Profile("dev")
public class LocalStorageConfig implements WebMvcConfigurer {

    @Value("${storage.local-path:uploads}")
    private String localPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get(localPath).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
