---
name: web-frontend
description: 校园植物地图 Web 前端开发规范。Phase 1 使用 Vue 3 + Vite + 高德地图 JS API 2.0，实现地图浏览、植物标记点、详情页、图片上传、管理后台（root/root 登录）。
---

# Web 前端开发规范

## 技术栈
- **框架**: Vue 3 + TypeScript（Composition API + `<script setup>`）
- **构建**: Vite 5
- **地图**: 高德地图 JS API 2.0（`@amap/amap-jsapi-loader`，直接调用无需额外封装）
- **HTTP**: Axios
- **路由**: Vue Router 4
- **UI**: Tailwind CSS（MVP 阶段不引入组件库）

## 项目结构

frontend/
├── public/
├── src/
│   ├── components/
│   │   ├── MapView.vue          ← 地图组件（核心）
│   │   └── ImageUpload.vue      ← 图片上传
│   ├── pages/
│   │   ├── HomePage.vue         ← 首页（地图）
│   │   ├── PlantDetailPage.vue  ← 植物详情页
│   │   └── AdminPage.vue        ← 管理页（含登录 + 上传）
│   ├── services/
│   │   └── api.ts               ← API 调用封装
│   ├── router/
│   │   └── index.ts             ← 路由配置
│   └── App.vue
├── .env                         ← VITE_API_BASE_URL, VITE_AMAP_KEY, VITE_AMAP_SECURITY_CODE
└── vite.config.ts

## 高德地图初始化

```vue
<!-- src/components/MapView.vue -->
<template>
  <div id="map-container" class="w-full h-screen"></div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import AMapLoader from '@amap/amap-jsapi-loader';
import { api } from '@/services/api';

const router = useRouter();
const mapInstance = ref<AMap.Map | null>(null);

onMounted(async () => {
  const AMap = await AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY,
    version: '2.0',
  });

  const map = new AMap.Map('map-container', {
    zoom: 16,
    center: [116.397, 39.916], // GCJ-02 坐标
  });
  mapInstance.value = map;
  await loadPlants(map);
});

const loadPlants = async (map: AMap.Map) => {
  const { data } = await api.getPlants();
  data.data.forEach((plant: any) => {
    const marker = new AMap.Marker({
      position: [plant.longitude, plant.latitude],
      title: plant.name,
      map,
    });
    marker.on('click', () => {
      router.push(`/plant/${plant.id}`);
    });
  });
};
</script>
```

## API 调用封装

```typescript
// src/services/api.ts
import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
});

client.interceptors.request.use((config) => {
  if (config.url?.startsWith('/api/admin')) {
    const token = localStorage.getItem('admin_token');
    if (token) config.headers['X-Admin-Token'] = token;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('admin_token');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const api = {
  login: (username: string, password: string) =>
    client.post('/api/auth/login', { username, password }),

  getPlants: (category?: string) =>
    client.get('/api/plants', { params: { category } }),

  getPlantDetail: (id: number) =>
    client.get(`/api/plants/${id}`),

  createPlant: (form: FormData) =>
    client.post('/api/admin/plants', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  uploadImage: (id: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return client.post(`/api/admin/plants/${id}/images`, form);
  },
};
```

## 注意事项
- `.env` 文件需配置：
  - `VITE_API_BASE_URL` — 后端地址（开发期 `http://localhost:8080`）
  - `VITE_AMAP_KEY` — 高德 JS API Key
  - `VITE_AMAP_SECURITY_CODE` — 高德 JS API 2.0 安全密钥
- 所有坐标后端已转为 GCJ-02，前端直接使用，不做二次转换
- 图片上传到后端，后端存入阿里云 OSS，数据库存 OSS URL
