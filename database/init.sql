-- ============================================================
-- 校园植物地图 — 数据库初始化（Phase 1，专注中国人民大学）
-- ============================================================
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. 树木个体表（每棵树独立记录，哈希 ID）
CREATE TABLE plant_species (
    id          VARCHAR(64) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,      -- 如 樱花-01
    category    VARCHAR(50) NOT NULL,        -- 如 樱花
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 位置表（一树一位置，含专属图片）
CREATE TABLE plant_location (
    id          SERIAL PRIMARY KEY,
    species_id  VARCHAR(64) NOT NULL REFERENCES plant_species(id) ON DELETE CASCADE,
    name        VARCHAR(100),                -- 位置描述
    image       VARCHAR(500),                -- 专属图片 URL
    geom        GEOMETRY(Point, 4326) NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    latitude    DOUBLE PRECISION NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_loc_geom ON plant_location USING GIST(geom);

-- 3. 图片库（按类型分组，无专属图片时备选）
CREATE TABLE plant_image (
    id          SERIAL PRIMARY KEY,
    category    VARCHAR(50) NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_img_category ON plant_image(category);

-- 4. 季节表（记录植物观赏季节）
CREATE TABLE plant_season (
    id       SERIAL PRIMARY KEY,
    category VARCHAR(255) NOT NULL,
    season   VARCHAR(4) NOT NULL
);

-- 种子数据
INSERT INTO plant_species (id, name, category, description) VALUES
('a1b2c3d4', '银杏-01', '银杏', '活化石植物，扇形叶，秋季金黄，校园标志性景观树。'),
('e5f6g7h8', '香樟-01', '香樟', '常绿大乔木，全株有香气，树冠广阔遮荫效果好。'),
('i9j0k1l2', '樱花-01', '樱花', '春季开花，粉白色花团锦簇，花期约两周。');

INSERT INTO plant_location (species_id, name, geom, longitude, latitude) VALUES
('a1b2c3d4', '图书馆东侧', ST_SetSRID(ST_MakePoint(116.313226, 39.970598), 4326), 116.313226, 39.970598),
('e5f6g7h8', '教学楼前',   ST_SetSRID(ST_MakePoint(116.314500, 39.969200), 4326), 116.314500, 39.969200),
('i9j0k1l2', '操场旁',     ST_SetSRID(ST_MakePoint(116.312100, 39.971500), 4326), 116.312100, 39.971500);
