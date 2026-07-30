---
name: spring-boot-postgis
description: Spring Boot 3.x 整合 PostgreSQL + PostGIS + 高德地图 API 的开发规范。涵盖 Hibernate Spatial、坐标转换（高德 API）、空间查询、GeoJSON 输出、图片 OSS 存储。API 设计同时兼容 Web 前端和微信小程序。
---

# Spring Boot + PostGIS 开发规范

## 依赖配置（pom.xml）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-spatial</artifactId>
</dependency>
<dependency>
    <groupId>net.postgis</groupId>
    <artifactId>postgis-jdbc</artifactId>
    <version>2023.1.0</version>
</dependency>
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.4</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

## 配置文件（application.yml）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/plant_mvp?stringtype=unspecified
    username: postgres
    password: postgres
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        spatial:
          enabled: true
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB

amap:
  key: ${AMAP_KEY:your-amap-key-here}
  security-code: ${AMAP_SECURITY_CODE:}

aliyun:
  oss:
    endpoint: ${OSS_ENDPOINT}
    bucket: ${OSS_BUCKET}
    access-key-id: ${OSS_ACCESS_KEY_ID}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET}
    cdn-domain: ${OSS_CDN_DOMAIN}

server:
  port: 8080
```

## 实体类空间字段映射

```java
@Entity
@Table(name = "plant_location")
public class PlantLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "geom", columnDefinition = "geometry(Point, 4326)")
    private Point geom;  // org.locationtech.jts.geom.Point

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    // WGS-84 存库，GCJ-02 对外
}
```

## 高德地图坐标转换

```java
@Service
public class AmapService {

    @Autowired
    private AmapConfig amapConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "https://restapi.amap.com/v3";

    public List<double[]> batchWgs84ToGcj02(List<double[]> coords) {
        if (coords.isEmpty()) return List.of();
        String locations = coords.stream()
                .map(c -> c[0] + "," + c[1])
                .collect(Collectors.joining("|"));
        String url = String.format(
            "%s/assistant/coordinate/convert?key=%s&locations=%s&coordsys=gps",
            BASE_URL, amapConfig.getKey(), locations);
        JSONObject resp = restTemplate.getForObject(url, JSONObject.class);
        if (!"1".equals(resp.getString("status"))) {
            throw new RuntimeException("高德坐标转换失败");
        }
        String[] results = resp.getString("locations").split(";");
        List<double[]> converted = new ArrayList<>();
        for (String r : results) {
            String[] parts = r.split(",");
            converted.add(new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])});
        }
        return converted;
    }

    public String reverseGeocode(double lng, double lat) {
        String url = String.format(
            "%s/geocode/regeo?key=%s&location=%s,%s&extensions=base",
            BASE_URL, amapConfig.getKey(), lng, lat);
        JSONObject resp = restTemplate.getForObject(url, JSONObject.class);
        if ("1".equals(resp.getString("status"))) {
            return resp.getJSONObject("regeocode").getString("formatted_address");
        }
        return null;
    }
}
```

## 空间查询 Repository

```java
@Query(value = """
    SELECT l.*,
        ST_Distance(
            l.geom::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
        ) as distance
    FROM plant_location l
    WHERE ST_DWithin(
        l.geom::geography,
        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
        :radiusMeters
    )
    AND l.status != '已移除'
    ORDER BY distance
    """, nativeQuery = true)
List<PlantLocation> findNearby(
    @Param("lng") double lng,
    @Param("lat") double lat,
    @Param("radiusMeters") int radiusMeters
);
```

## API 设计（5 个，Web 和小程序共用）

```
POST /api/auth/login          → 登录（root/root → 返回 Token）
GET  /api/plants              → 植物列表（含 GCJ-02 坐标）
GET  /api/plants/{id}         → 植物详情（物种信息 + 图片列表 + 逆地理编码地址）
POST /api/admin/plants        → 新增植物（multipart）
POST /api/admin/plants/{id}/images → 追加图片（multipart: file）

Phase 2 追加：
GET  /api/plants/by-code/{code}  → 按编号查询植物（供小程序扫码使用）
```

## 分层规范

- **Controller**: 只负责参数校验、调用 Service、返回 Result
- **Service**: 业务逻辑、事务管理、坐标转换
- **Repository**: 数据访问、空间查询
- **Entity**: 与表一一对应，禁止在 Entity 里写业务逻辑
- **VO/DTO**: 对外暴露的数据结构，Controller 层做 Entity → VO 转换
