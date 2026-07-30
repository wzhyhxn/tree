# 校园植物地图项目 — Agent 指令

你是校园植物地图项目的全栈开发助手。当前处于 **Phase 1：Web 网站**。

## 技术栈
- 前端：Vue 3 + TypeScript + Vite + 高德 JS API 2.0
- 后端：Spring Boot 3.x + Spring Data JPA
- 数据库：PostgreSQL 15 + PostGIS 3.3
- 地图服务：高德地图 Web API（坐标转换、逆地理编码）
- 图片存储：阿里云 OSS（后端中转上传，CDN 加速）

## 核心原则

1. **坐标系严格分离**：数据库存 WGS-84，API 返回前调用高德 API 转 GCJ-02，前端（Web/小程序）直接使用不做二次转换
2. **API 设计前端无关**：Phase 1 简易 Token 认证（root/root），Phase 2 升级为微信登录，Token 机制不变
3. **空间查询用 PostGIS**：所有地理距离、范围查询用 `ST_DWithin`、`ST_Distance`，禁止 Java 里做距离计算
4. **图片走后端中转上 OSS**：前端 FormData 上传到后端，后端存入阿里云 OSS，数据库存 OSS URL，前端直接用 URL 展示
5. **渐进式开发**：先跑通核心链路（地图展示 → 标记点 → 详情 → 上传），Phase 2 再拆小程序

## Phase 1 开发顺序（严格遵守）
1. Docker 跑 PostgreSQL + 执行 init.sql
2. Spring Boot 项目骨架 + 3 个 Entity + 3 个 Repository
3. AmapConfig + AmapService（坐标转换 + 逆地理编码）
4. 5 个 API（登录、列表、详情、新增植物、追加图片）
5. Vue 3 前端骨架 + 高德地图初始化
6. 地图标记点展示 + 点击详情
7. 图片上传（FormData → 后端 → OSS → 返回 CDN URL）

## 当用户提到以下任务时，自动加载对应 Skill
- "项目结构"、"技术选型"、"架构" → 读取 `.pi/skills/campus-plant-arch/SKILL.md`
- "前端页面"、"Vue"、"高德地图"、"地图" → 读取 `.pi/skills/web-frontend/SKILL.md`
- "后端 API"、"Spring Boot"、"JPA"、"PostGIS"、"坐标转换" → 读取 `.pi/skills/spring-boot-postgis/SKILL.md`
- "图片上传"、"OSS"、"阿里云"、"CDN" → 读取 `.pi/skills/aliyun-oss/SKILL.md`
- "小程序"、"微信"、"Phase 2" → 读取 `.pi/skills/wechat-miniprogram/SKILL.md`

## 禁止做的事
- 不要把图片二进制存在数据库里
- 不要在前端直接调用 PostgreSQL
- 不要手写 WGS-84 → GCJ-02 转换算法（调高德 API）
- 不要把高德 Key 写死在代码里（用环境变量 `${AMAP_KEY}`）
- Phase 1 不碰微信小程序、不碰微信登录
- Phase 1 用简易登录（root/root），不接微信/手机号等外部认证
- 不要把图片流经后端后存本地磁盘（必须上 OSS）

## 🔴 文档同步（必须遵守）
每次修改项目代码后，**必须同步更新**对应的文档：

| 改动类型 | 需更新的文件 |
|----------|-------------|
| 前端页面/组件/样式 | `readme.md`（Phase 1 对应章节）、`DESIGN.md`（如有设计变化）、`.pi/skills/web-frontend/SKILL.md` |
| 后端 API/Service/Entity | `readme.md`（API 设计章节）、`.pi/skills/spring-boot-postgis/SKILL.md` |
| 数据库表/种子数据 | `database/init.sql`、`readme.md`（数据库章节）、`.pi/skills/campus-plant-arch/SKILL.md` |
| 配置/密钥/依赖 | `key.md`、`readme.md`（配置章节）、`application.yml` 或 `.env` |
| 新增文件/目录 | `readme.md`（项目结构章节） |

> ⚠️ **代码和文档不一致比没有文档更糟糕。每次改动结束前，主动告知用户已同步更新了哪些文档。**

## 常用命令

```bash
# 启动后端
cd backend && ./mvnw spring-boot:run

# 启动前端
cd frontend && npm run dev

# 数据库迁移
cd database && psql -U postgres -d plant_mvp -f init.sql
```
