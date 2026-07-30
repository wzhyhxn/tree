<template>
  <div v-if="plant" class="min-h-screen bg-gray-50">
    <header class="nav-blur sticky top-0 z-10 h-12 flex items-center px-4">
      <button @click="$router.back()" class="flex items-center gap-1 text-gray-500 hover:text-green-700"><span class="text-lg">←</span><span class="text-sm">返回</span></button>
      <span class="flex-1 text-center text-sm font-semibold text-gray-700 truncate px-2">{{ plant.name }}</span>
      <div style="width:48px"></div>
    </header>

    <img v-if="plant.fallbackImage" :src="plant.fallbackImage" class="w-full h-56 object-cover" />
    <div v-else class="w-full h-48 bg-gradient-to-br from-green-50 to-emerald-100 flex items-center justify-center">
      <span class="text-5xl">{{ emoji(plant.category) }}</span>
    </div>

    <div class="card mx-4 mt-4 p-4">
      <div class="flex items-start justify-between">
        <div><h1 class="text-xl font-bold">{{ plant.name }}</h1><p class="text-sm text-gray-400 mt-0.5">{{ plant.category }}</p></div>
      </div>
      <div class="mt-3 flex flex-wrap gap-2">
        <span class="tag bg-gray-100 text-gray-600">{{ plant.category }}</span>
        <span class="tag bg-gray-100 text-gray-600">📍 {{ plant.locationName }}</span>
      </div>
      <p class="text-sm text-gray-600 mt-4 leading-relaxed">{{ plant.description || '暂无描述' }}</p>
    </div>

    <div class="card mx-4 mt-3 mb-6 overflow-hidden">
      <div class="px-4 py-3 flex items-center gap-2 border-b border-gray-100"><span>🗺️</span><span class="text-sm font-semibold text-gray-600">位置地图</span></div>
      <div id="mini-map" class="w-full h-56"></div>
    </div>
  </div>
  <div v-else class="min-h-screen bg-gray-50 flex items-center justify-center">
    <div class="text-center"><div class="skeleton w-12 h-12 rounded-full mx-auto mb-3"></div><div class="skeleton w-40 h-4 mx-auto"></div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import AMapLoader from '@amap/amap-jsapi-loader';
import { api } from '@/services/api';

const SPECIES_EMOJI: Record<string, string> = { '银杏': '🌿', '香樟': '🌳', '樱花': '🌸' };
const emoji = (c: string) => SPECIES_EMOJI[c] || '🌱';

const route = useRoute();
const plant = ref<any>(null);

onMounted(async () => {
  try {
    const { data } = await api.getPlantDetail(route.params.id as string);
    plant.value = data.data;
    await nextTick();
    initMiniMap(data.data.longitude, data.data.latitude);
  } catch { console.error('加载详情失败'); }
});

const initMiniMap = async (lng: number, lat: number) => {
  const AMap = await AMapLoader.load({ key: import.meta.env.VITE_AMAP_KEY, version: '2.0' });
  const map = new AMap.Map('mini-map', {
    zoom: 17, center: [lng, lat], resizeEnable: true,
    dragEnable: false, zoomEnable: false, scrollWheel: false,
    doubleClickZoom: false, keyboardEnable: false,
  });
  new AMap.Marker({ position: [lng, lat], map });
  new AMap.Circle({ center: [lng, lat], radius: 30, map, fillColor: '#52b788', fillOpacity: 0.2, strokeColor: '#2d6a4f', strokeWeight: 1.5 });
};
</script>
