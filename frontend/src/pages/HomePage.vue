<template>
  <div class="home">
    <!-- ========== Header ========== -->
    <header class="nav-blur header">
      <div class="header-left">
        <span class="header-icon">🌳</span>
        <h1 class="header-title">校园植物地图</h1>
        <span class="header-subtitle">中国人民大学</span>
      </div>
      <div class="header-right">
        <button
          class="btn btn-ghost season-trigger"
          @click="seasonOpen = !seasonOpen"
        >
          <span>{{ activeSeason ? activeSeason + '季' : '四季' }}</span>
          <span class="season-chevron" :class="{ open: seasonOpen }">▾</span>
        </button>
        <router-link to="/admin" class="btn btn-ghost admin-link">管理</router-link>
      </div>
    </header>

    <!-- Season dropdown -->
    <div v-if="seasonOpen" class="season-dropdown card-elevated">
      <button
        v-for="s in seasons" :key="s.value"
        class="season-option"
        :class="{ active: activeSeason === s.value }"
        @click="selectSeason(s.value)"
      >
        <span class="season-option-icon">{{ s.icon }}</span>
        <span class="season-option-label">{{ s.label }}</span>
      </button>
      <div class="divider"></div>
      <button class="season-option" @click="selectSeason('')">
        <span class="season-option-icon">⊖</span>
        <span class="season-option-label">显示全部</span>
      </button>
    </div>

    <!-- ========== Search Panel (left side) ========== -->
    <div class="search-panel">
      <!-- Search input -->
      <div class="card search-box">
        <span class="search-icon">🔍</span>
        <input
          v-model="keyword"
          @input="doSearch"
          placeholder="搜索树木名称…"
          class="search-input"
        />
        <button
          v-if="keyword"
          @click="clearSearch"
          class="search-clear"
          aria-label="清除搜索"
        >
          ✕
        </button>
      </div>

      <!-- Search results -->
      <div v-if="results.length && keyword" class="card search-results">
        <div
          v-for="p in results" :key="p.id"
          class="search-result-item"
          :class="{ active: selected?.id === p.id }"
          @click="selectPlant(p)"
        >
          <span class="search-result-emoji">{{ plantEmoji(p.category) }}</span>
          <div class="search-result-info">
            <span class="search-result-name">{{ p.name }}</span>
            <span class="search-result-location">{{ p.locationName }}</span>
          </div>
        </div>
      </div>

      <!-- Mini detail card (when a plant is selected) -->
      <div v-if="selected" class="card mini-detail">
        <div class="mini-detail-top">
          <span class="mini-detail-emoji">{{ plantEmoji(selected.category) }}</span>
          <div class="mini-detail-info">
            <h3 class="mini-detail-name">{{ selected.name }}</h3>
            <p class="mini-detail-category latin">{{ selected.category }}</p>
          </div>
          <button @click="openDetail" class="btn btn-warm mini-detail-link">
            详情 →
          </button>
        </div>
        <div class="mini-detail-meta">
          <span>📍 {{ selected.locationName }}</span>
        </div>
        <img
          v-if="selected.fallbackImage"
          :src="selected.fallbackImage"
          class="mini-detail-image"
          alt=""
        />
      </div>

      <!-- Empty state: no plant selected -->
      <div v-if="!selected && !keyword" class="search-hint">
        <p>搜索树木名称，或在地图上点击标记</p>
      </div>
    </div>

    <!-- ========== Map ========== -->
    <MapView ref="mapRef" @select-plant="openFromMap" />

    <!-- ========== Detail overlay ========== -->
    <div v-if="showOverlay" class="overlay-backdrop" @click.self="showOverlay = false">
      <div class="detail-overlay card-elevated">
        <div class="detail-overlay-header">
          <div class="detail-overlay-title">
            <span class="detail-overlay-emoji">{{ plantEmoji(detail?.category) }}</span>
            <h2 class="detail-overlay-name">{{ detail?.name }}</h2>
          </div>
          <button @click="showOverlay = false" class="detail-overlay-close" aria-label="关闭">✕</button>
        </div>
        <div class="detail-overlay-body">
          <img
            v-if="detail?.fallbackImage"
            :src="detail.fallbackImage"
            class="detail-overlay-image"
            alt=""
          />
          <div v-else class="detail-overlay-image-placeholder">
            <span class="detail-overlay-emoji-lg">{{ plantEmoji(detail?.category) }}</span>
          </div>

          <p class="detail-overlay-desc">{{ detail?.description || '暂无简介' }}</p>

          <div class="detail-overlay-grid">
            <div class="detail-overlay-field">
              <span class="detail-overlay-label">类型</span>
              <span class="detail-overlay-value">{{ detail?.category }}</span>
            </div>
            <div class="detail-overlay-field">
              <span class="detail-overlay-label">位置</span>
              <span class="detail-overlay-value">{{ detail?.locationName }}</span>
            </div>
            <div class="detail-overlay-field">
              <span class="detail-overlay-label">坐标</span>
              <span class="detail-overlay-value mono">
                {{ detail?.longitude?.toFixed(4) }}, {{ detail?.latitude?.toFixed(4) }}
              </span>
            </div>
            <div class="detail-overlay-field">
              <span class="detail-overlay-label">编号</span>
              <span class="detail-overlay-value">{{ detail?.name }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import MapView from '@/components/MapView.vue';
import { api } from '@/services/api';

const mapRef = ref<any>(null);

const PLANT_EMOJI: Record<string, string> = {
  '银杏': '🍂', '香樟': '🌳', '桂花': '🌼', '樱花': '🌸', '悬铃木': '🍁'
};
const plantEmoji = (c: string) => PLANT_EMOJI[c] || '🌱';

const seasons = [
  { value: '春', label: '春花', icon: '🌸' },
  { value: '夏', label: '夏荫', icon: '🌿' },
  { value: '秋', label: '秋叶', icon: '🍂' },
  { value: '冬', label: '冬枝', icon: '❄️' },
];

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
      const { data } = await api.getPlants(keyword.value, undefined);
      results.value = data.data || [];
    } catch { /* silent */ }
  }, 200);
};

const selectSeason = (season: string) => {
  if (activeSeason.value === season) return;
  activeSeason.value = season;
  seasonOpen.value = false;
  mapRef.value?.refreshBySeason(season || undefined);
};

const clearSearch = () => {
  keyword.value = '';
  results.value = [];
};

const selectPlant = (p: any) => {
  selected.value = p;
  results.value = [];
  mapRef.value?.zoomTo(p.id);
};

const openDetail = async () => {
  if (!selected.value) return;
  try {
    const { data } = await api.getPlantDetail(selected.value.id);
    detail.value = data.data;
    showOverlay.value = true;
  } catch { /* silent */ }
};

const openFromMap = async (plant: any) => {
  try {
    const { data } = await api.getPlantDetail(plant.id);
    selected.value = data.data;
    mapRef.value?.zoomTo(plant.id);
  } catch { /* silent */ }
};

watch(showOverlay, (v) => {
  document.body.style.overflow = v ? 'hidden' : '';
});
</script>

<style scoped>
/* ─── Layout ─── */
.home {
  width: 100vw; height: 100vh;
  position: relative;
  overflow: hidden;
}

/* ─── Header ─── */
.header {
  position: absolute; top: 0; left: 0; right: 0;
  height: var(--nav-height); z-index: 20;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 var(--space-4);
}
.header-left {
  display: flex; align-items: center; gap: var(--space-2);
}
.header-icon { font-size: 1.125rem; }
.header-title {
  font-size: var(--text-base); font-weight: 700;
  letter-spacing: 0.02em;
}
.header-subtitle {
  font-size: var(--text-xs); color: var(--color-text-tertiary);
  margin-left: var(--space-1);
  padding-left: var(--space-2);
  border-left: 1px solid var(--color-border);
}
.header-right {
  display: flex; align-items: center; gap: var(--space-1);
}

.season-trigger {
  font-size: var(--text-xs);
  gap: 0.15rem;
}
.season-chevron {
  font-size: 0.6rem; transition: transform 0.2s;
}
.season-chevron.open { transform: rotate(180deg); }
.admin-link { font-size: var(--text-xs); }

/* ─── Season dropdown ─── */
.season-dropdown {
  position: absolute; top: calc(var(--nav-height) + 0.25rem);
  right: var(--space-4); z-index: 25;
  padding: var(--space-2);
  min-width: 8rem;
  animation: fadeIn 0.15s ease;
}
.season-option {
  display: flex; align-items: center; gap: var(--space-3);
  width: 100%; padding: 0.5rem 0.75rem;
  border: none; border-radius: var(--radius);
  background: none; cursor: pointer;
  font-family: var(--font-body); font-size: var(--text-sm);
  color: var(--color-text-secondary);
  transition: all 0.12s;
}
.season-option:hover { background: var(--color-accent-soft); color: var(--color-accent); }
.season-option.active {
  background: var(--color-accent-soft);
  color: var(--color-accent);
  font-weight: 600;
}
.season-option-icon { font-size: 1rem; width: 1.25rem; text-align: center; }
.season-option-label { flex: 1; text-align: left; }

/* ─── Search panel ─── */
.search-panel {
  position: absolute; top: calc(var(--nav-height) + 0.5rem);
  left: var(--space-3); z-index: 15;
  width: 17rem;
  display: flex; flex-direction: column; gap: var(--space-2);
  pointer-events: none;
}
.search-panel > * { pointer-events: auto; }

/* Search box */
.search-box {
  display: flex; align-items: center; gap: var(--space-2);
  padding: 0.5rem 0.75rem;
}
.search-icon { font-size: var(--text-sm); flex-shrink: 0; color: var(--color-text-tertiary); }
.search-input {
  flex: 1; border: none; outline: none;
  font-family: var(--font-body); font-size: var(--text-sm);
  color: var(--color-text); background: transparent;
  min-width: 0;
}
.search-input::placeholder { color: var(--color-text-tertiary); }
.search-clear {
  flex-shrink: 0; border: none; background: none;
  color: var(--color-text-tertiary); cursor: pointer;
  font-size: var(--text-xs); padding: 0.15rem 0.3rem;
  border-radius: var(--radius-sm);
}
.search-clear:hover { color: var(--color-text); background: var(--color-border-light); }

/* Search results */
.search-results {
  max-height: 14rem; overflow-y: auto;
  animation: fadeIn 0.12s ease;
}
.search-result-item {
  display: flex; align-items: center; gap: var(--space-3);
  padding: 0.6rem 0.75rem;
  cursor: pointer;
  transition: background 0.1s;
  border-bottom: 1px solid var(--color-border-light);
}
.search-result-item:last-child { border-bottom: none; }
.search-result-item:hover { background: var(--color-accent-soft); }
.search-result-item.active { background: var(--color-accent-soft); }
.search-result-emoji { font-size: 1.25rem; flex-shrink: 0; }
.search-result-info {
  flex: 1; min-width: 0;
  display: flex; flex-direction: column; gap: 0.1rem;
}
.search-result-name {
  font-size: var(--text-sm); font-weight: 600;
  color: var(--color-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.search-result-location {
  font-size: var(--text-xs); color: var(--color-text-tertiary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* Mini detail card */
.mini-detail {
  padding: var(--space-3);
  display: flex; flex-direction: column; gap: var(--space-2);
  animation: fadeIn 0.2s ease;
}
.mini-detail-top {
  display: flex; align-items: flex-start; gap: var(--space-3);
}
.mini-detail-emoji { font-size: 1.75rem; flex-shrink: 0; line-height: 1; }
.mini-detail-info { flex: 1; min-width: 0; }
.mini-detail-name {
  font-family: var(--font-display); font-size: var(--text-md);
  font-weight: 700; color: var(--color-text);
  line-height: 1.3;
}
.mini-detail-category {
  font-size: var(--text-xs);
  margin-top: 0.1rem;
}
.mini-detail-link {
  font-size: var(--text-xs); flex-shrink: 0;
  padding: 0.25rem 0.5rem;
}
.mini-detail-meta {
  font-size: var(--text-xs); color: var(--color-text-secondary);
  display: flex; align-items: center; gap: var(--space-2);
}
.mini-detail-image {
  width: 100%; height: 5.5rem;
  object-fit: cover; border-radius: var(--radius);
}

/* Search hint */
.search-hint {
  text-align: center;
  padding: var(--space-6) var(--space-4);
  font-size: var(--text-xs); color: var(--color-text-tertiary);
  line-height: 1.6;
}

/* ─── Detail overlay ─── */
.detail-overlay {
  width: min(60%, 42rem); max-height: 80vh;
  display: flex; flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.25s ease;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(1rem); } to { opacity: 1; transform: translateY(0); } }

.detail-overlay-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border-light);
}
.detail-overlay-title {
  display: flex; align-items: center; gap: var(--space-3);
}
.detail-overlay-emoji { font-size: 1.5rem; }
.detail-overlay-name {
  font-size: var(--text-lg); font-weight: 700;
}
.detail-overlay-close {
  width: 2rem; height: 2rem;
  border: none; border-radius: 50%;
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: var(--text-sm); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.detail-overlay-close:hover { background: var(--color-border); color: var(--color-text); }

.detail-overlay-body {
  overflow-y: auto; padding: var(--space-5);
  display: flex; flex-direction: column; gap: var(--space-4);
}
.detail-overlay-image {
  width: 100%; height: 14rem;
  object-fit: cover; border-radius: var(--radius-lg);
}
.detail-overlay-image-placeholder {
  width: 100%; height: 10rem;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-accent-soft), #F0F7F2);
  display: flex; align-items: center; justify-content: center;
}
.detail-overlay-emoji-lg { font-size: 3rem; }

.detail-overlay-desc {
  font-size: var(--text-sm); color: var(--color-text-secondary);
  line-height: 1.8;
}
.detail-overlay-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-3);
}
.detail-overlay-field {
  background: var(--color-bg);
  border-radius: var(--radius);
  padding: var(--space-3);
  display: flex; flex-direction: column; gap: 0.25rem;
}
.detail-overlay-label {
  font-size: var(--text-xs); color: var(--color-text-tertiary);
}
.detail-overlay-value {
  font-size: var(--text-sm); font-weight: 600; color: var(--color-text);
}

/* ─── Mobile ─── */
@media (max-width: 768px) {
  .search-panel {
    width: calc(100vw - 1.5rem);
  }
  .detail-overlay {
    width: calc(100vw - 2rem);
    max-height: 85vh;
    margin: var(--space-3);
  }
  .header-subtitle { display: none; }
}
</style>
