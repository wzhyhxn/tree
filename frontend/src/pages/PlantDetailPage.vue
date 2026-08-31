<template>
  <div v-if="plant" class="detail-page">
    <!-- Header -->
    <header class="nav-blur detail-header">
      <button @click="$router.back()" class="btn btn-ghost back-btn">
        <span>←</span> <span>返回</span>
      </button>
      <span class="detail-header-title">{{ plant.name }}</span>
      <div class="detail-header-spacer"></div>
    </header>

    <!-- Hero image -->
    <div class="detail-hero">
      <img v-if="plant.fallbackImage" :src="plant.fallbackImage" class="detail-hero-img" alt="" />
      <div v-else class="detail-hero-placeholder">
        <span class="detail-hero-emoji">{{ plantEmoji(plant.category) }}</span>
      </div>
    </div>

    <!-- Content -->
    <div class="detail-content">
      <div class="card detail-card">
        <div class="detail-card-header">
          <div>
            <h1 class="detail-name">{{ plant.name }}</h1>
            <p class="detail-category latin">{{ plant.category }}</p>
          </div>
          <span class="tag">{{ plant.category }}</span>
        </div>

        <div class="detail-meta">
          <span>📍 {{ plant.locationName }}</span>
        </div>

        <div class="divider"></div>

        <p class="detail-desc">{{ plant.description || '暂无详细介绍。' }}</p>
      </div>

      <div class="card detail-map-card">
        <div class="detail-map-header">
          <span>🗺️</span>
          <span class="detail-map-title">校园位置</span>
        </div>
        <div id="mini-map" class="detail-mini-map"></div>
      </div>
    </div>

    <!-- Footer coordinates -->
    <div class="detail-coords mono">
      经度 {{ plant.longitude?.toFixed(4) }} &nbsp;·&nbsp; 纬度 {{ plant.latitude?.toFixed(4) }}
    </div>
  </div>

  <div v-else class="detail-loading">
    <div class="skeleton" style="width:3rem;height:3rem;border-radius:50%;margin:0 auto 1rem;"></div>
    <div class="skeleton" style="width:10rem;height:1rem;margin:0 auto;"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import AMapLoader from '@amap/amap-jsapi-loader';
import { api } from '@/services/api';

const PLANT_EMOJI: Record<string, string> = {
  '银杏': '🍂', '香樟': '🌳', '桂花': '🌼', '樱花': '🌸', '悬铃木': '🍁'
};
const plantEmoji = (c: string) => PLANT_EMOJI[c] || '🌱';

const route = useRoute();
const plant = ref<any>(null);

onMounted(async () => {
  try {
    const { data } = await api.getPlantDetail(route.params.id as string);
    plant.value = data.data;
    await nextTick();
    initMiniMap(data.data.longitude, data.data.latitude);
  } catch {
    console.error('加载详情失败');
  }
});

const initMiniMap = async (lng: number, lat: number) => {
  const AMap = await AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY,
    version: '2.0',
  });
  const map = new AMap.Map('mini-map', {
    zoom: 17,
    center: [lng, lat],
    resizeEnable: true,
    dragEnable: false,
    zoomEnable: false,
    scrollWheel: false,
    doubleClickZoom: false,
    keyboardEnable: false,
  });
  new AMap.Marker({ position: [lng, lat], map });
  new AMap.Circle({
    center: [lng, lat], radius: 30, map,
    fillColor: '#1F5C3A', fillOpacity: 0.12,
    strokeColor: '#1F5C3A', strokeWeight: 1.5,
  });
};
</script>

<style scoped>
.detail-page {
  min-height: 100vh; background: var(--color-bg);
  display: flex; flex-direction: column;
}

/* Header */
.detail-header {
  position: sticky; top: 0; z-index: 10;
  height: var(--nav-height);
  display: flex; align-items: center;
  padding: 0 var(--space-4);
}
.back-btn { font-size: var(--text-sm); gap: 0.25rem; }
.detail-header-title {
  flex: 1; text-align: center;
  font-family: var(--font-display); font-size: var(--text-sm);
  font-weight: 700; color: var(--color-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  padding: 0 var(--space-2);
}
.detail-header-spacer { width: 3.5rem; }

/* Hero */
.detail-hero {
  width: 100%; height: 16rem;
  overflow: hidden;
}
.detail-hero-img {
  width: 100%; height: 100%; object-fit: cover;
}
.detail-hero-placeholder {
  width: 100%; height: 100%;
  background: linear-gradient(160deg, var(--color-accent-soft) 0%, #EAF3EC 50%, #F5F3EE 100%);
  display: flex; align-items: center; justify-content: center;
}
.detail-hero-emoji { font-size: 4rem; }

/* Content */
.detail-content {
  padding: var(--space-4);
  display: flex; flex-direction: column; gap: var(--space-3);
  max-width: 42rem; width: 100%; margin: 0 auto;
}

/* Info card */
.detail-card {
  padding: var(--space-5);
}
.detail-card-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  gap: var(--space-3);
}
.detail-name {
  font-size: var(--text-xl); font-weight: 700;
}
.detail-category {
  font-size: var(--text-sm);
  margin-top: 0.2rem;
}
.detail-meta {
  margin-top: var(--space-3);
  font-size: var(--text-sm); color: var(--color-text-secondary);
}
.divider { margin: var(--space-4) 0; }
.detail-desc {
  font-size: var(--text-sm); color: var(--color-text-secondary);
  line-height: 1.9; letter-spacing: 0.01em;
}

/* Map card */
.detail-map-card {
  overflow: hidden;
}
.detail-map-header {
  display: flex; align-items: center; gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border-light);
  font-size: var(--text-sm); font-weight: 600;
  color: var(--color-text-secondary);
}
.detail-mini-map {
  width: 100%; height: 12rem;
}

/* Coords footer */
.detail-coords {
  text-align: center;
  padding: var(--space-4) var(--space-4) var(--space-8);
  font-size: var(--text-xs); color: var(--color-text-tertiary);
}

/* Loading */
.detail-loading {
  min-height: 100vh; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: var(--color-bg);
}
</style>
