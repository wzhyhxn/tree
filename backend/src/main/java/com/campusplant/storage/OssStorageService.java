package com.campusplant.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CannedAccessControlList;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * 阿里云 OSS 存储 — 生产环境使用。
 * 需设置环境变量: OSS_ENDPOINT, OSS_ACCESS_KEY_ID, OSS_ACCESS_KEY_SECRET, OSS_BUCKET, OSS_CDN_DOMAIN
 */
@Service
@Profile("prod")
public class OssStorageService implements StorageService {

    private OSS ossClient;

    @Value("${storage.oss.endpoint:}")
    private String endpoint;

    @Value("${storage.oss.access-key-id:}")
    private String accessKeyId;

    @Value("${storage.oss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${storage.oss.bucket:}")
    private String bucket;

    @Value("${storage.oss.cdn-domain:}")
    private String cdnDomain;

    @PostConstruct
    public void init() {
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        System.out.println("[Storage] OSS 存储: bucket=" + bucket + " cdn=" + cdnDomain);
    }

    @PreDestroy
    public void shutdown() {
        if (ossClient != null) ossClient.shutdown();
    }

    @Override
    public String upload(String objectName, InputStream inputStream) throws IOException {
        ossClient.putObject(bucket, objectName, inputStream);
        ossClient.setObjectAcl(bucket, objectName, CannedAccessControlList.PublicRead);
        return String.format("https://%s/%s", cdnDomain, objectName);
    }
}
