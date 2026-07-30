---
name: aliyun-oss
description: 阿里云 OSS 集成指南。当用户实现图片上传、OSS 存储、CDN 配置时使用。Phase 1 采用后端中转上传模式（浏览器 → 后端 → OSS），Phase 2 小程序改为直传（STS 临时凭证）。
---

# 阿里云 OSS 集成规范

## Bucket 配置
- Bucket 名称：`campus-plant-images`
- 地域：与 ECS/RDS 同地域（oss-cn-beijing）
- 存储类型：标准存储
- 读写权限：私有（通过 RAM 子账号 AccessKey 访问）

## CORS 配置（必须，否则浏览器/小程序上传失败）
```xml
<CORSConfiguration>
  <CORSRule>
    <AllowedOrigin>*</AllowedOrigin>
    <AllowedMethod>PUT</AllowedMethod>
    <AllowedMethod>POST</AllowedMethod>
    <AllowedHeader>*</AllowedHeader>
  </CORSRule>
</CORSConfiguration>
```

## Phase 1：后端中转上传

```java
@Service
public class OssService {

    private final OSS ossClient;

    @Value("${aliyun.oss.bucket}")
    private String bucket;

    @Value("${aliyun.oss.cdn-domain}")
    private String cdnDomain;

    public OssService(
        @Value("${aliyun.oss.endpoint}") String endpoint,
        @Value("${aliyun.oss.access-key-id}") String accessKeyId,
        @Value("${aliyun.oss.access-key-secret}") String accessKeySecret) {
        this.ossClient = new OSSClientBuilder()
            .build(endpoint, accessKeyId, accessKeySecret);
    }

    public void upload(String objectName, InputStream inputStream) {
        ossClient.putObject(bucket, objectName, inputStream);
    }

    public String getCdnUrl(String objectName) {
        return String.format("https://%s/%s", cdnDomain, objectName);
    }
}
```

## Phase 2：STS 临时凭证（小程序直传用）

```java
@GetMapping("/api/oss/sts")
public Result<Map<String, String>> getStsToken() {
    return Result.ok(Map.of(
        "accessKeyId", "...",
        "accessKeySecret", "...",
        "securityToken", "...",
        "expiration", "..."
    ));
}
```

## 图片处理（CDN 缩略图）
- OSS 控制台开启"图片处理"
- 创建样式 `thumbnail` → 缩放宽度 800px

## 安全规范
- 永远不要在前端暴露主账号 AccessKey
- Phase 1 用 RAM 子账号（仅 PutObject 权限）
- Phase 2 切 STS 临时凭证
- 设置 Bucket 防盗链 Referer 白名单
