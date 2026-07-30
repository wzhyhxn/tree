# 校园植物地图 — 阿里云部署指南

> Phase 1 Web 网站 | Ubuntu 22.04 | ECS 2C1G 30G + RDS PostgreSQL 50G

---

## 一、部署架构

```
浏览器
  │ HTTPS (443) / HTTP (80)
  ▼
ECS 安全组（放行 80, 443, 22）
  │
  ▼
Nginx :80/:443
  ├── /          → /var/www/campus-plant （Vue 前端静态文件）
  └── /api/*     → 127.0.0.1:8080 （Spring Boot）
                    │
                    ├── RDS PostgreSQL 15 + PostGIS 3.3（内网连接）
                    ├── 阿里云 OSS（图片存储，已有）
                    └── 高德地图 API（坐标转换）
```

---

## 二、阿里云产品清单

| 产品                 | 用途                     | 规格                          |
| ------------------ | ---------------------- | --------------------------- |
| **ECS**            | 运行 Nginx + Spring Boot | 2C1G，30G 系统盘，Ubuntu 22.04   |
| **RDS PostgreSQL** | 数据库                    | PG 15，50G 存储，与 ECS 同地域同 VPC |
| **OSS**            | 图片存储（已有）               | `campus-plant-images`，北京节点  |
| **CDN**（可选）        | 图片加速                   | 绑定 OSS bucket               |

### 数据库版本

| 组件         | 版本                     |
| ---------- | ---------------------- |
| PostgreSQL | **15.x**（RDS 当前默认 15）  |
| PostGIS    | **3.3**（RDS 创建后通过插件开启） |

> 与项目 `database/init.sql` 和 `pom.xml` 中 `postgis-jdbc 2023.1.0` 对应。

---

## 三、RDS PostgreSQL 配置

### 3.1 购买 RDS

阿里云控制台 → RDS → 创建实例：

| 配置项       | 值                     |
| --------- | --------------------- |
| **数据库类型** | PostgreSQL            |
| **版本**    | **15**                |
| **系列**    | 基础版（单节点，开发测试够用）       |
| **规格**    | 1C1G（最低配，够用）          |
| **存储**    | 50G SSD               |
| **网络**    | 与 ECS **同 VPC**（内网互通） |
| **白名单**   | 添加 ECS 内网 IP 或 VPC 网段 |

### 3.2 创建数据库和扩展

RDS 实例就绪后，用 DMS 或 psql 连接：

```sql
-- 创建数据库
CREATE DATABASE plant_mvp;

-- 连接 plant_mvp 库后，开启 PostGIS 扩展
CREATE EXTENSION postgis;

-- 验证
SELECT PostGIS_Version();
-- 应返回 3.3.x
```

### 3.3 导入初始化数据

将本地 `database/init.sql` 上传到 ECS，再从 ECS 导入 RDS：

```bash
# 本地 → ECS
scp database/init.sql root@<ECS_IP>:/opt/campus-plant/init/

# ECS 上安装 psql 客户端
apt install -y postgresql-client

# 导入 RDS（替换为 RDS 内网地址、账号、密码）
psql -h <RDS内网地址> -U <数据库账号> -d plant_mvp -f /opt/campus-plant/init/init.sql
```

### 3.4 验证数据

```bash
psql -h <RDS内网地址> -U <数据库账号> -d plant_mvp
```

```sql
\dt
-- 应显示：plant_species, plant_location, plant_image, plant_season

SELECT id, name, latin_name FROM plant_species;
-- 应返回 5 行种子数据

\q
```

---

## 四、ECS 环境初始化

### 4.1 连接服务器

```bash
ssh root@<ECS公网IP>
```

### 4.2 创建 swap（1G 内存建议）

```bash
dd if=/dev/zero of=/swapfile bs=1M count=2048
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

free -h
# Swap 应显示 2.0Gi
```

### 4.3 安装基础依赖

```bash
apt update && apt upgrade -y
apt install -y openjdk-17-jdk nginx curl wget unzip postgresql-client

java -version
# openjdk version "17.x"
```

> Ubuntu 22.04 源为 JDK 17，与 Spring Boot 3.x 完全兼容。`postgresql-client` 仅用于调试连接 RDS。

### 4.4 创建目录

```bash
mkdir -p /opt/campus-plant/app
mkdir -p /var/www/campus-plant
```

---

## 五、后端部署（Spring Boot）

### 5.1 本地打包

```bash
cd campus-plant-map/backend
./mvnw clean package -DskipTests
```

产出：`target/campus-plant-backend-0.1.0.jar`

### 5.2 上传到 ECS

```bash
scp target/campus-plant-backend-0.1.0.jar root@<ECS_IP>:/opt/campus-plant/app/
```

### 5.3 创建环境变量文件

在 ECS 上创建 `/opt/campus-plant/app/.env`：

```bash
cat > /opt/campus-plant/app/.env << 'EOF'
# RDS PostgreSQL（替换为实际地址和密码）
SPRING_DATASOURCE_URL=jdbc:postgresql://<RDS内网地址>:5432/plant_mvp?stringtype=unspecified
SPRING_DATASOURCE_USERNAME=<数据库账号>
SPRING_DATASOURCE_PASSWORD=<数据库密码>

# 高德地图（替换为实际 Key）
AMAP_KEY=<你的高德Key>

# 阿里云 OSS（替换为实际密钥）
OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
OSS_BUCKET=campus-plant-images
OSS_ACCESS_KEY_ID=<你的AK>
OSS_ACCESS_KEY_SECRET=<你的SK>
OSS_CDN_DOMAIN=campus-plant-images.oss-cn-beijing.aliyuncs.com

SERVER_PORT=8080
EOF
```

> - `<RDS内网地址>`：RDS 控制台 → 基本信息 → 内网地址，类似 `rm-xxx.pg.rds.aliyuncs.com`
> - `<数据库账号>`：创建 RDS 时设置的高权限账号
> - JVM `-Xmx256m`：1G 内存下给 Java 堆 256M，加上 Metaspace 和栈，约 350M

### 5.4 创建 Systemd 服务

```bash
cat > /etc/systemd/system/campus-plant.service << 'EOF'
[Unit]
Description=Campus Plant Map Backend
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/campus-plant/app
EnvironmentFile=/opt/campus-plant/app/.env
ExecStart=/usr/bin/java -Xms128m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -jar /opt/campus-plant/app/campus-plant-backend-0.1.0.jar
Restart=always
RestartSec=10
StandardOutput=append:/opt/campus-plant/app/app.log
StandardError=append:/opt/campus-plant/app/app.log
MemoryMax=512M

[Install]
WantedBy=multi-user.target
EOF
```

### 5.5 启动后端

```bash
systemctl daemon-reload
systemctl enable campus-plant
systemctl start campus-plant

systemctl status campus-plant
tail -f /opt/campus-plant/app/app.log
```

> 首次启动 Hibernate 验证表结构 + 连接 RDS，约 10-15 秒。

### 5.6 验证

```bash
curl http://127.0.0.1:8080/api/plants
# {"code":200,"msg":"success","data":[...]}
```

---

## 六、前端部署

### 6.1 生产环境配置

创建 `frontend/.env.production`（不要修改 `.env`，那是开发环境用的）：

```bash
cat > frontend/.env.production << 'EOF'
VITE_API_BASE_URL=/api
VITE_AMAP_KEY=<你的高德Key>
VITE_AMAP_SECURITY_CODE=<你的高德安全密钥>
EOF
```

> Vite 在 `npm run build` 时自动读取 `.env.production`。`/api` 会被 Nginx 反向代理到后端 8080。

### 6.2 构建并上传

```bash
cd campus-plant-map/frontend
npm install
npm run build
# 产出：dist/ 目录

---
```

## 七、Nginx 配置

### 7.1 创建站点配置

```bash
cat > /etc/nginx/sites-available/campus-plant << 'EOF'
server {
    listen 80;
    server_name _;

    root /var/www/campus-plant;
    index index.html;

    access_log /var/log/nginx/campus-plant-access.log;
    error_log  /var/log/nginx/campus-plant-error.log;

    client_max_body_size 5m;

    # Vue Router history 模式
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_read_timeout 30s;
        client_max_body_size 5m;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
    }
}
EOF
```

### 7.2 启用

```bash
ln -sf /etc/nginx/sites-available/campus-plant /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

nginx -t
systemctl reload nginx
systemctl enable nginx
```

---

## 八、HTTPS 配置（有域名时）

```bash
apt install -y certbot python3-certbot-nginx

# 先把 nginx 配置中 server_name 改为真实域名，再执行：
certbot --nginx -d your-domain.com

# 验证自动续期
certbot renew --dry-run
```

---

## 九、安全组与网络配置

### ECS 安全组（入方向）

| 端口  | 协议  | 来源        | 说明    |
| --- | --- | --------- | ----- |
| 22  | TCP | 你的IP/32   | SSH   |
| 80  | TCP | 0.0.0.0/0 | HTTP  |
| 443 | TCP | 0.0.0.0/0 | HTTPS |

> 8080 不对外，仅 Nginx 走 127.0.0.1 回环代理。

### RDS 白名单

| 来源        | 说明                                 |
| --------- | ---------------------------------- |
| ECS 内网 IP | 或 ECS 所在 VPC 网段（如 `172.16.0.0/12`） |

> ECS 和 RDS 同 VPC 则内网互通，不走公网流量。

---

## 十、OSS CORS 配置

阿里云控制台 → OSS → `campus-plant-images` → 数据安全 → 跨域设置 → 创建规则：

| 来源                 | 允许 Methods     | 允许 Headers |
| ------------------ | -------------- | ---------- |
| `http://<ECS公网IP>` | PUT, POST, GET | *          |
| `https://你的域名`     | PUT, POST, GET | *          |

---

## 十一、验证部署

### 11.1 命令行

```bash
# 后端 API
curl -s http://127.0.0.1:8080/api/plants | head -c 300

# 通过 Nginx 访问
curl -s http://127.0.0.1/api/plants | head -c 300

# 前端首页
curl -s http://127.0.0.1/ | head -c 200

# 内存
free -h
```

### 11.2 浏览器

1. 打开 `http://<ECS公网IP>`
2. 地图首页显示 5 个植物标记点（种子数据）
3. 点击标记点 → 详情页，有逆地理地址
4. `/login` → `root / root` 登录 → 新增植物 + 上传图片
5. 图片上传后，OSS bucket 中出现文件

---

## 十二、内存预算（1G ECS，数据库在 RDS）

| 进程               | 预计内存                           |
| ---------------- | ------------------------------ |
| Ubuntu 系统        | ~150M                          |
| Nginx            | ~50M                           |
| Spring Boot JVM  | ~350M（堆 256M + Metaspace 128M） |
| **合计**           | **~550M**                      |
| 剩余（buffer/cache） | ~450M                          |
| swap（缓冲）         | 2G                             |

> 数据库走 RDS 后，ECS 内存非常充裕，无需 Docker，余量充足。

---

## 十三、常用运维命令

```bash
# === 后端 ===
systemctl status campus-plant
systemctl restart campus-plant
journalctl -u campus-plant -f
tail -100 /opt/campus-plant/app/app.log

# === Nginx ===
systemctl reload nginx
tail -50 /var/log/nginx/campus-plant-access.log
tail -50 /var/log/nginx/campus-plant-error.log

# === RDS 数据库连接测试 ===
psql -h <RDS内网地址> -U <账号> -d plant_mvp

# === RDS 备份（在 ECS 上执行）===
pg_dump -h <RDS内网地址> -U <账号> -d plant_mvp \
  > /opt/campus-plant/backup_$(date +%Y%m%d_%H%M).sql

# === 系统监控 ===
free -h                    # 内存
df -h                      # 磁盘

# === 更新后端 ===
# 本地：
scp target/campus-plant-backend-0.1.0.jar root@<IP>:/opt/campus-plant/app/
ssh root@<IP> "systemctl restart campus-plant"

# === 更新前端 ===
# 本地：
cd frontend && npm run build
scp -r dist/* root@<IP>:/var/www/campus-plant/
```

---

## 十四、部署检查清单

| #   | 检查项                                                       | ✓   |
| --- | --------------------------------------------------------- | --- |
| 1   | RDS PostgreSQL 15 实例已创建，50G 存储                            |     |
| 2   | PostGIS 3.3 扩展已开启                                         |     |
| 3   | `init.sql` 已导入，`SELECT count(*) FROM plant_species;` 返回 5 |     |
| 4   | RDS 白名单已添加 ECS 内网 IP                                      |     |
| 5   | ECS 安全组放行 80、443、22                                       |     |
| 6   | swap 已创建，`free -h` 确认                                     |     |
| 7   | JDK 17 已安装                                                |     |
| 8   | `/opt/campus-plant/app/.env` 中 RDS 连接和 OSS 密钥正确           |     |
| 9   | `systemctl status campus-plant` 显示 `active (running)`     |     |
| 10  | `curl http://127.0.0.1:8080/api/plants` 返回 JSON           |     |
| 11  | 前端 `dist/` 已上传到 `/var/www/campus-plant/`                  |     |
| 12  | Nginx 配置生效，`nginx -t` 通过                                  |     |
| 13  | 浏览器 `http://<IP>` 地图首页正常                                  |     |
| 14  | 管理端 `root/root` 登录 + 上传图片，OSS 有文件                         |     |
| 15  | OSS CORS 规则已添加                                            |     |

---

> 📅 最后更新：2025-07-29 | Phase 1 | Ubuntu 22.04 | ECS 2C1G 30G | RDS PG15 50G
