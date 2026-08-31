package com.campusplant.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件存储抽象 — PlantService 只依赖此接口，不感知底层是本地还是 OSS。
 */
public interface StorageService {

    /** 上传文件，返回可访问的 URL */
    String upload(String objectName, InputStream inputStream) throws IOException;
}
