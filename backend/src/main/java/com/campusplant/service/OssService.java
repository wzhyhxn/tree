package com.campusplant.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class OssService {

    private OSS ossClient;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket}")
    private String bucket;

    @Value("${aliyun.oss.cdn-domain}")
    private String cdnDomain;

    @PostConstruct
    public void init() {
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    /**
     * 上传文件到 OSS
     */
    public void upload(String objectName, InputStream inputStream) {
        ossClient.putObject(bucket, objectName, inputStream);
    }

    /**
     * 拼接 CDN URL（数据库存这个）
     */
    public String getCdnUrl(String objectName) {
        return String.format("https://%s/%s", cdnDomain, objectName);
    }
}
