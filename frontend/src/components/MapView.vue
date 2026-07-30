<template>
  <div id="map-container" class="w-full h-full relative">
    <!-- 图层切换 -->
    <div class="absolute top-14 right-2 z-10 flex flex-col gap-1">
      <button v-for="l in layers" :key="l.value" @click="switchLayer(l.value)"
        class="text-xs px-2.5 py-1.5 rounded-lg font-medium transition-all border"
        :class="current === l.value
          ? 'bg-green-700 text-white border-green-700 shadow'
          : 'bg-white text-gray-600 border-gray-200 hover:border-green-400 shadow-sm'"
      >{{ l.label }}</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import AMapLoader from '@amap/amap-jsapi-loader';
import { api } from '@/services/api';

const emit = defineEmits<{ (e: 'selectPlant', plant: any): void }>();

let AMap: any = null;
let mapInstance: any = null;
let markers: any[] = [];
const current = ref<'satellite' | 'normal'>('satellite');
const layers = [
  { label: '🛰️ 卫星', value: 'satellite' as const },
  { label: '🗺️ 地图', value: 'normal' as const },
];

onMounted(async () => {
  AMap = await AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY, version: '2.0',
    plugins: ['AMap.Scale'],
  });

  const map = new AMap.Map('map-container', {
    zoom: 17, center: [116.313226, 39.970598],
    layers: [new AMap.TileLayer.Satellite()],
    resizeEnable: true, zooms: [16, 19], viewMode: '2D',
  });
  map.addControl(new AMap.Scale({ position: 'LB' }));
  map.setFeatures(['bg']);  // 仅底色，无文字无标注

  const campusSW = [116.304, 39.961], campusNE = [116.324, 39.980];
  map.on('moveend', () => {
    const c = map.getCenter();
    const [clng, clat] = [c.getLng(), c.getLat()];
    if (clng < campusSW[0] || clng > campusNE[0] || clat < campusSW[1] || clat > campusNE[1]) {
      map.setCenter([
        Math.max(campusSW[0], Math.min(campusNE[0], clng)),
        Math.max(campusSW[1], Math.min(campusNE[1], clat)),
      ]);
    }
  });

  mapInstance = map;
  await loadMarkers(map);
});

const loadMarkers = async (map: any, season?: string) => {
  try {
    const params: any = {};
    if (season) params.season = season;
    const { data } = await api.client.get('/plants', { params });
    const plants = data.data || [];

    plants.forEach((p: any) => {
      const content = document.createElement('div');
      content.style.cssText = 'display:flex;flex-direction:column;align-items:center;cursor:pointer;';
      content.innerHTML = `
        <div style="width:14px;height:14px;border-radius:50%;background:#2d6a4f;border:2px solid #fff;box-shadow:0 1px 3px rgba(0,0,0,0.3);"></div>
        <span style="margin-top:2px;font-size:11px;font-weight:600;color:#2d6a4f;white-space:nowrap;text-shadow:0 1px 2px rgba(255,255,255,0.8);">${p.name}</span>
      `;

      const marker = new AMap.Marker({
        position: [p.longitude, p.latitude],
        content,
        offset: new AMap.Pixel(-7, -25),
        zIndex: 100,
        map,
      });

      content.addEventListener('click', (e) => {
        e.stopPropagation();
        emit('selectPlant', p);
      });

      marker._plantId = p.id;
      marker._plantData = p;

      markers.push(marker);
    });
  } catch (err) {
    console.error('加载植物失败:', err);
  }
};

const switchLayer = (type: 'satellite' | 'normal') => {
  if (!AMap || !mapInstance) return;
  current.value = type;
  if (type === 'satellite') {
    mapInstance.setLayers([new AMap.TileLayer.Satellite()]);
  } else {
    mapInstance.setLayers([new AMap.TileLayer()]);
  }
  mapInstance.setFeatures(['bg']);
};

const refreshBySeason = async (season: string) => {
  if (!mapInstance) return;
  markers.forEach(m => m.setMap(null));
  markers = [];
  await loadMarkers(mapInstance, season || undefined);
};

const zoomTo = (id: string) => {
  if (!mapInstance || !id) return;
  const m = markers.find(mk => mk._plantId === id);
  if (m) mapInstance.setZoomAndCenter(18, m.getPosition());
};

defineExpose({ refreshBySeason, zoomTo });
</script>

<style scoped>
#map-container { width: 100%; min-height: 100vh; }
</style>
