<template>
  <div id="map-container">
    <!-- Layer toggle -->
    <div class="layer-toggle">
      <button
        v-for="l in layers" :key="l.value"
        @click="switchLayer(l.value)"
        class="layer-btn"
        :class="{ active: current === l.value }"
      >
        {{ l.label }}
      </button>
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
  { label: '卫星', value: 'satellite' as const },
  { label: '地图', value: 'normal' as const },
];

onMounted(async () => {
  AMap = await AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY,
    version: '2.0',
    plugins: ['AMap.Scale'],
  });

  const map = new AMap.Map('map-container', {
    zoom: 17,
    center: [116.313226, 39.970598],
    layers: [new AMap.TileLayer.Satellite()],
    resizeEnable: true,
    zooms: [16, 19],
    viewMode: '2D',
  });
  map.addControl(new AMap.Scale({ position: 'LB' }));
  map.setFeatures(['bg']);

  // Clamp to campus bounds
  const campusSW = [116.304, 39.961];
  const campusNE = [116.324, 39.980];
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
    const { data } = await api.getPlants(undefined, season);
    const plants = data.data || [];

    plants.forEach((p: any) => {
      const content = document.createElement('div');
      content.style.cssText =
        'display:flex;flex-direction:column;align-items:center;cursor:pointer;';
      content.innerHTML = `
        <div style="width:12px;height:12px;border-radius:50%;background:#1F5C3A;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,0.25);"></div>
        <span style="margin-top:2px;font-size:10px;font-weight:700;color:#1A1A1A;white-space:nowrap;text-shadow:0 1px 2px rgba(255,255,255,0.9);letter-spacing:0.02em;">${p.name}</span>
      `;

      const marker = new AMap.Marker({
        position: [p.longitude, p.latitude],
        content,
        offset: new AMap.Pixel(-6, -22),
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
  markers.forEach((m) => m.setMap(null));
  markers = [];
  await loadMarkers(mapInstance, season || undefined);
};

const zoomTo = (id: string) => {
  if (!mapInstance || !id) return;
  const m = markers.find((mk) => mk._plantId === id);
  if (m) mapInstance.setZoomAndCenter(18, m.getPosition());
};

defineExpose({ refreshBySeason, zoomTo });
</script>

<style scoped>
#map-container {
  width: 100%; height: 100%;
  position: absolute; inset: 0;
}

/* Layer toggle */
.layer-toggle {
  position: absolute; top: calc(var(--nav-height) + 0.5rem);
  right: var(--space-3); z-index: 10;
  display: flex; gap: 2px;
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius);
  padding: 2px;
  box-shadow: var(--shadow-sm);
}
.layer-btn {
  padding: 0.35rem 0.65rem;
  border: none; border-radius: var(--radius-sm);
  background: transparent;
  font-family: var(--font-body); font-size: var(--text-xs);
  font-weight: 600; color: var(--color-text-secondary);
  cursor: pointer; transition: all 0.12s;
}
.layer-btn:hover { color: var(--color-accent); }
.layer-btn.active {
  background: var(--color-accent); color: #fff;
}
</style>
