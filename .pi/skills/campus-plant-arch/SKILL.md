---
name: campus-plant-arch
description: 校园植物地图项目的整体架构指南。当用户询问项目结构、技术选型、模块划分、开发顺序、部署方案时使用。Phase 1 为 Web 网站（root/root 简易登录），Phase 2 拆分为微信小程序 + 升级认证。
---

# 校园植物地图 — 项目架构

## 技术栈

| 层级 | Phase 1（Web 网站） | Phase 2（小程序） |
|------|-------------------|------------------|
| **前端** | Vue 3 + TypeScript + Vite + 高德 JS API 2.0 | 微信小程序原生（map 组件） |
| **后端** | Spring Boot 3.x + Spring Data JPA | 业务代码不变，Phase 2 增设认证拦截器保护 POST |
| **数据库** | PostgreSQL 15 + PostGIS 3.3 | 不变 |
| **地图服务** | 高德地图 Web API（坐标转换、逆地理编码） | 不变 |
| **图片存储** | 阿里云 OSS（后端中转上传） | 小程序直传 OSS（STS 临时凭证） |

## 核心模块
1. **地图展示模块** — 标记点、聚合、筛选、详情弹窗
2. **植物库模块** — 物种信息、图片、物候记录
3. **位置管理模块** — 单株植物编号、坐标、状态、二维码（Phase 2）
4. **管理后台模块** — 植物信息录入、图片上传、Excel 导入

## 数据库核心表（Phase 1 仅 3 张）

```sql
-- init.sql
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE plant_species (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    latin_name  VARCHAR(200),
    category    VARCHAR(20),       -- 乔木/灌木/草本/藤本
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plant_location (
    id          SERIAL PRIMARY KEY,
    species_id  INTEGER NOT NULL REFERENCES plant_species(id),
    code        VARCHAR(50) NOT NULL UNIQUE,   -- 编号 YX-001
    name        VARCHAR(100),                   -- 位置描述
    geom        GEOMETRY(Point, 4326) NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    latitude    DOUBLE PRECISION NOT NULL,
    status      VARCHAR(20) DEFAULT '健康',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_loc_geom ON plant_location USING GIST(geom);

CREATE TABLE plant_image (
    id          SERIAL PRIMARY KEY,
    location_id INTEGER NOT NULL REFERENCES plant_location(id) ON DELETE CASCADE,
    image_url   VARCHAR(500) NOT NULL,
    is_primary  BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 种子数据
INSERT INTO plant_species (name, latin_name, category, description) VALUES
('银杏', 'Ginkgo biloba', '乔木', '活化石植物，扇形叶，秋季金黄'),
('香樟', 'Cinnamomum camphora', '乔木', '常绿大乔木，全株有香气'),
('桂花', 'Osmanthus fragrans', '灌木', '秋季开花，香气浓郁');

INSERT INTO plant_location (species_id, code, name, geom, longitude, latitude) VALUES
(1, 'YX-001', '图书馆东侧', ST_SetSRID(ST_MakePoint(116.397, 39.916), 4326), 116.397, 39.916),
(2, 'XZ-001', '教学楼前', ST_SetSRID(ST_MakePoint(116.398, 39.917), 4326), 116.398, 39.917);
```

## 开发顺序
1. Docker 跑 PostgreSQL + 执行 init.sql
2. Spring Boot 项目骨架 + 3 个 Entity + Repository
3. AmapService（坐标转换）+ 5 个 API（含登录）
4. Web 前端：高德地图 + 标记点展示
5. Web 前端：植物详情页 + 图片上传
6. （Phase 2）拆分为两个微信小程序

## 坐标系处理原则
- 存：WGS-84（EPSG:4326）
- 转：后端调用高德 API `/v3/assistant/coordinate/convert` 转 GCJ-02
- 传：API 返回 GCJ-02，Web 和小程序的地图组件均使用 GCJ-02
- 高德 Key：配置在 `application.yml`，通过环境变量 `AMAP_KEY` 注入

## 部署架构

浏览器/小程序 → HTTPS → Nginx → Spring Boot → PostgreSQL/RDS
                    ↓                 ↓
              阿里云 OSS（图片存储，CDN 加速）
