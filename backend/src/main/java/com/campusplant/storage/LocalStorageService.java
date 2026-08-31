package com.campusplant.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储 — 开发环境使用。
 */
@Service
@Profile("dev")
public class LocalStorageService implements StorageService {

    private Path uploadDir;

    @Value("${storage.local-path:uploads}")
    private String localPath;

    @PostConstruct
    public void init() throws IOException {
        uploadDir = Paths.get(localPath).toAbsolutePath();
        Files.createDirectories(uploadDir);
        System.out.println("[Storage] 本地存储: " + uploadDir);
    }

    @Override
    public String upload(String objectName, InputStream inputStream) throws IOException {
        Path target = uploadDir.resolve(objectName);
        Files.createDirectories(target.getParent());
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + objectName;
    }
}
