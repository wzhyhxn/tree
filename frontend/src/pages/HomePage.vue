<template>
  <div class="relative w-full h-screen overflow-hidden">
    <!-- 顶部导航条 -->
    <header class="nav-blur absolute top-0 left-0 right-0 z-20 h-12 flex items-center px-4 gap-2">
      <span class="text-lg">🌳</span>
      <h1 class="text-base font-bold text-gray-900">校园植物地图</h1>
      <div class="flex-1"></div>
      <router-link to="/admin" class="text-sm text-gray-500 hover:text-green-700 px-3 py-1.5 rounded-lg hover:bg-green-50">⚙️ 管理</router-link>
    </header>

    <!-- 左侧搜索面板 -->
    <div class="absolute top-14 left-3 z-20 w-64 space-y-2">
      <!-- 搜索框 -->
      <div class="card p-2 flex items-center gap-2">
        <span class="text-sm text-gray-400 pl-1">🔍</span>
        <input
          v-model="keyword"
          @input="doSearch"
          placeholder="搜索树木名称..."
          class="flex-1 text-sm border-none outline-none bg-transparent text-gray-700"
        />
        <span v-if="keyword" @click="clearSearch" class="text-gray-300 cursor-pointer text-sm">✕</span>
      </div>

      <!-- 季节选择器 -->
      <div class="card p-2">
        <button @click="seasonOpen = !seasonOpen"
          class="w-full flex items-center justify-between text-sm text-gray-500 px-2 py-1 rounded-lg hover:bg-gray-50">
          <span>{{ activeSeason ? '🌿 季节浏览 · ' + activeSeason : '🌿 季节浏览' }}</span>
          <span class="text-xs text-gray-300">{{ seasonOpen ? '▲' : '▼' }}</span>
        </button>
        <div v-if="seasonOpen" class="mt-2 px-2 space-y-1">
          <label v-for="s in ['春','夏','秋','冬']" :key="s"
            class="flex items-center gap-2 py-1 cursor-pointer text-sm"
            :class="activeSeason === s ? 'text-green-700 font-medium' : 'text-gray-500'"
            @click="selectSeason(s)">
            <span class="w-4 h-4 rounded-full border-2 flex items-center justify-center"
              :class="activeSeason === s ? 'border-green-600' : 'border-gray-300'">
              <span v-if="activeSeason === s" class="w-2 h-2 rounded-full bg-green-600"></span>
            </span>
            {{ s }}
          </label>
          <button v-if="activeSeason" @click="selectSeason('')" class="text-xs text-gray-400 pl-6">清除</button>
        </div>
      </div>

      <!-- 搜索结果列表 -->
      <div v-if="results.length" class="card max-h-48 overflow-y-auto">
        <div
          v-for="p in results"
          :key="p.id"
          @click="selectPlant(p)"
          class="flex items-center gap-2 px-3 py-2 cursor-pointer hover:bg-green-50 transition-colors border-b border-gray-50 last:border-0"
          :class="{ 'bg-green-50': selected?.id === p.id }"
        >
          <span class="text-lg">{{ emoji(p.category) }}</span>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-gray-800 truncate">{{ p.name }}</p>
            <p class="text-xs text-gray-400 truncate">{{ p.locationName }}</p>
          </div>
        </div>
      </div>

      <!-- 迷你详情卡 -->
      <div v-if="selected" class="card p-3 space-y-2">
        <div class="flex items-start justify-between">
          <div class="flex items-center gap-2">
            <span class="text-2xl">{{ emoji(selected.category) }}</span>
            <div>
              <p class="font-semibold text-gray-800 text-sm">{{ selected.name }}</p>
              <p class="text-xs text-gray-400">{{ selected.category }}</p>
            </div>
          </div>
          <button @click="openDetail" class="text-xs text-green-700 font-medium hover:underline whitespace-nowrap">
            树木详情 →
          </button>
        </div>
        <div class="flex gap-2 text-xs text-gray-500">
          <span>📍 {{ selected.locationName }}</span>
        </div>
        <img v-if="selected.fallbackImage" :src="selected.fallbackImage" class="w-full h-24 object-cover rounded-lg" />
      </div>
    </div>

    <!-- 地图 -->
    <MapView ref="mapRef" @select-plant="openFromMap" />

    <!-- 遮罩层 + 详情弹窗 -->
    <div v-if="showOverlay" class="absolute inset-0 z-30 flex items-center justify-center" style="background:rgba(0,0,0,0.35)">
      <div class="bg-white rounded-2xl shadow-2xl overflow-hidden flex flex-col" style="width:60%;max-height:80vh">
        <!-- 弹窗头部 -->
        <div class="flex items-center justify-between px-5 py-3 border-b border-gray-100">
          <div class="flex items-center gap-2">
            <span class="text-xl">{{ emoji(detail?.category) }}</span>
            <span class="font-bold text-gray-800">{{ detail?.name }}</span>
          </div>
          <button @click="showOverlay = false" class="text-gray-400 hover:text-gray-600 text-lg leading-none">✕</button>
        </div>
        <!-- 弹窗内容 -->
        <div class="overflow-y-auto p-5 space-y-4">
          <img v-if="detail?.fallbackImage" :src="detail.fallbackImage" class="w-full h-48 object-cover rounded-xl" />
          <p class="text-sm text-gray-600 leading-relaxed">{{ detail?.description || '暂无简介' }}</p>
          <div class="grid grid-cols-2 gap-2 text-xs text-gray-500">
            <div class="bg-gray-50 rounded-lg p-2"><span class="text-gray-400">编号</span><br><span class="font-semibold text-gray-700">{{ detail?.name }}</span></div>
            <div class="bg-gray-50 rounded-lg p-2"><span class="text-gray-400">类型</span><br><span class="font-semibold text-gray-700">{{ detail?.category }}</span></div>
            <div class="bg-gray-50 rounded-lg p-2"><span class="text-gray-400">位置</span><br><span class="font-semibold text-gray-700">{{ detail?.locationName }}</span></div>
            <div class="bg-gray-50 rounded-lg p-2"><span class="text-gray-400">坐标</span><br><span class="font-semibold text-gray-700">{{ detail?.longitude?.toFixed(4) }}, {{ detail?.latitude?.toFixed(4) }}</span></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import MapView from '@/components/MapView.vue';

const mapRef = ref<any>(null);
import { api } from '@/services/api';

const SPECIES_EMOJI: Record<string, string> = { '银杏': '🌿', '香樟': '🌳', '樱花': '🌸' };
const emoji = (c: string) => SPECIES_EMOJI[c] || '🌱';

const keyword = ref('');
const results = ref<any[]>([]);
const selected = ref<any>(null);
const showOverlay = ref(false);
const detail = ref<any>(null);
const seasonOpen = ref(false);
const activeSeason = ref('');

let searchTimer: any = null;

const doSearch = () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(async () => {
    if (!keyword.value.trim()) { results.value = []; return; }
    try {
      const { data } = await api.getPlants(keyword.value);
      results.value = data.data || [];
    } catch {}
  }, 200);
};

// 季节切换
const selectSeason = (season: string) => {
  if (activeSeason.value === season) return;
  activeSeason.value = season;
  seasonOpen.value = false;
  // 通知 MapView 刷新标记
  mapRef.value?.refreshBySeason(season);
};

const clearSearch = () => {
  keyword.value = '';
  results.value = [];
};

const selectPlant = (p: any) => {
  selected.value = p;
  mapRef.value?.zoomTo(p.id);
};

const openDetail = async () => {
  if (!selected.value) return;
  try {
    const { data } = await api.getPlantDetail(selected.value.id);
    detail.value = data.data;
    showOverlay.value = true;
  } catch {}
};

// 地图标记点击 → 显示迷你详情卡
const openFromMap = async (plant: any) => {
  try {
    const { data } = await api.getPlantDetail(plant.id);
    selected.value = data.data;
    mapRef.value?.zoomTo(plant.id);
  } catch {}
};

watch(showOverlay, (v) => {
  document.body.style.overflow = v ? 'hidden' : '';
});
</script>
