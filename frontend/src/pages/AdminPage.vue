<template>
  <div class="min-h-screen bg-gray-50 flex">
    <!-- ========== 左侧边栏 ========== -->
    <aside class="w-56 bg-white border-r border-gray-200 flex flex-col shrink-0">
      <div class="p-4 border-b border-gray-100">
        <router-link to="/" class="flex items-center gap-2 text-gray-500 hover:text-green-700 text-sm">
          <span>←</span> <span>返回地图</span>
        </router-link>
        <h2 class="text-base font-bold text-gray-800 mt-3">🌳 管理后台</h2>
      </div>
      <nav class="flex-1 p-3 space-y-1">
        <button
          v-for="t in tabs"
          :key="t.key"
          @click="switchTab(t.key)"
          class="w-full text-left px-3 py-2 rounded-lg text-sm font-medium transition-colors"
          :class="activeTab === t.key ? 'bg-green-50 text-green-700' : 'text-gray-600 hover:bg-gray-50'"
        >{{ t.label }}</button>
      </nav>
      <div v-if="token" class="p-3 border-t border-gray-100">
        <button @click="logout" class="text-xs text-gray-400 hover:text-red-500 w-full text-left">退出登录</button>
      </div>
    </aside>

    <!-- ========== 右侧内容区 ========== -->
    <main class="flex-1 overflow-y-auto" style="max-height:100vh">
      <!-- 登录 -->
      <div v-if="!token" class="flex items-center justify-center h-full">
        <div class="card w-80 p-6">
          <div class="text-center mb-6"><span class="text-4xl">🌳</span><h2 class="text-lg font-bold mt-2">管理员登录</h2></div>
          <div class="space-y-3">
            <input v-model="username" class="input-field" placeholder="用户名" @keyup.enter="doLogin" />
            <input v-model="password" class="input-field" type="password" placeholder="密码" @keyup.enter="doLogin" />
            <p v-if="error" class="text-red-500 text-xs text-center">{{ error }}</p>
            <button @click="doLogin" class="btn-primary w-full">登录</button>
          </div>
        </div>
      </div>

      <!-- 树木目录 -->
      <div v-if="token && activeTab === 'list'" class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-bold text-gray-800">📋 树木目录 <span class="text-sm text-gray-400 font-normal ml-1">({{ plants.length }} 棵)</span></h3>
        </div>
        <div class="space-y-2">
          <div v-for="p in plants" :key="p.id"
            class="card px-5 py-3 flex items-center gap-4">
            <span class="text-2xl">{{ emoji(p.category) }}</span>
            <div class="flex-1 min-w-0">
              <p class="font-semibold text-gray-800 text-sm">{{ p.name }}</p>
              <p class="text-xs text-gray-400">{{ p.category }} · {{ p.locationName }}</p>
            </div>
            <div class="flex gap-2">
              <button @click="startEdit(p)" class="text-xs px-3 py-1.5 rounded-lg bg-blue-50 text-blue-600 hover:bg-blue-100 transition-colors">✏️ 修改</button>
              <button @click="confirmDelete(p)" class="text-xs px-3 py-1.5 rounded-lg bg-red-50 text-red-500 hover:bg-red-100 transition-colors">🗑 删除</button>
            </div>
          </div>
          <p v-if="!plants.length" class="text-gray-400 text-sm text-center py-8">暂无树木</p>
        </div>
      </div>

      <!-- 新增 / 编辑 -->
      <div v-if="token && (activeTab === 'add' || activeTab === 'edit')" class="p-6 max-w-3xl">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-bold text-gray-800">{{ editing ? '✏️ 修改树木' : '➕ 新增树木' }}</h3>
          <span v-if="editing" class="text-xs bg-amber-50 text-amber-700 px-3 py-1 rounded-full font-medium">
            正在修改：{{ editing.name }}
          </span>
        </div>

        <!-- 框1：类型 + 简介 -->
        <div class="card p-4 mb-4 space-y-3">
          <div>
            <label class="text-xs text-gray-400 mb-1 block">类型（物种）</label>
            <input v-model="form.category" class="input-field" placeholder="如 银杏、樱花"
              @input="onCategoryChange" list="category-list" />
            <datalist id="category-list">
              <option v-for="c in categories" :key="c" :value="c" />
            </datalist>
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">简介 <span class="text-gray-300">（填写类型后自动匹配，可修改）</span></label>
            <textarea v-model="form.description" class="input-field" rows="3" placeholder="植物特征、物候等信息"
              @input="descriptionDirty = true"></textarea>
          </div>
        </div>

        <!-- 框2：位置 + 照片 + 坐标 -->
        <div class="flex flex-col md:flex-row gap-4 mb-4">
          <!-- 左侧：位置表单 -->
          <div class="card p-4 space-y-3" :class="showMapPicker ? 'md:w-1/2' : 'w-full'" style="transition:all 0.3s ease">
            <div>
              <label class="text-xs text-gray-400 mb-1 block">位置描述</label>
              <input v-model="form.locationName" class="input-field" placeholder="如图书馆东侧" />
            </div>
            <div>
              <label class="text-xs text-gray-400 mb-1 block">照片</label>
              <label class="upload-zone block cursor-pointer">
                <input type="file" accept="image/*" class="hidden" @change="onFileChange" />
                <span class="text-3xl">{{ file ? '✅' : '📷' }}</span>
                <p class="text-sm text-gray-500 mt-2">{{ file ? file.name : '点击上传' }}</p>
              </label>
            </div>
            <div class="flex items-center justify-between">
              <label class="text-xs text-gray-400">📍 坐标</label>
              <button @click="toggleMapPicker"
                class="text-xs px-3 py-1 rounded-lg bg-green-50 text-green-600 hover:bg-green-100 transition-colors">
                {{ showMapPicker ? '收起地图' : '地图选点' }}
              </button>
            </div>
            <div class="grid grid-cols-2 gap-2">
              <div><label class="text-xs text-gray-400 block mb-1">经度</label>
                <input v-model.number="form.longitude" type="number" step="0.000001" class="input-field text-sm font-mono" /></div>
              <div><label class="text-xs text-gray-400 block mb-1">纬度</label>
                <input v-model.number="form.latitude" type="number" step="0.000001" class="input-field text-sm font-mono" /></div>
            </div>
          </div>

          <!-- 右侧：地图卡片（桌面端右侧，手机端下方） -->
          <div v-if="showMapPicker"
            class="card p-3 overflow-hidden shrink-0"
            :class="isMobile ? 'w-full' : 'md:w-1/2'"
            :style="{ transition: 'all 0.3s ease' }">
            <div id="picker-map" class="w-full h-72 rounded-lg"></div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="flex gap-3">
          <button v-if="editing" @click="cancelEdit" class="flex-1 py-2.5 rounded-lg border border-gray-200 text-gray-500 text-sm font-medium hover:bg-gray-50">取消</button>
          <button @click="submitPlant" class="btn-primary flex-1" :disabled="submitting">{{ submitting ? '提交中...' : (editing ? '保存修改' : '提交') }}</button>
        </div>
        <p v-if="message" class="text-sm text-center mt-3" :class="ok ? 'text-green-600' : 'text-red-500'">{{ message }}</p>
      </div>

      <!-- 季节管理 -->
      <div v-if="token && activeTab === 'season'" class="p-6 max-w-3xl">
        <h3 class="text-lg font-bold text-gray-800 mb-4">🌿 分季节浏览推荐</h3>
        <p class="text-sm text-gray-400 mb-4">设定每个季节展示哪些植物类型，用户端按季节筛选。</p>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
          <div v-for="(s, si) in seasonLabels" :key="s" class="card p-3">
            <h4 class="text-sm font-bold text-gray-700 mb-2">{{ s }}</h4>
            <label v-for="cat in categories" :key="cat"
              @click="toggleSeasonCat(cat, s)"
              class="flex items-center gap-2 py-1 cursor-pointer text-sm"
              :class="isSeasonChecked(cat, s) ? 'text-green-700 font-medium' : 'text-gray-400'">
              <span class="w-4 h-4 border-2 rounded flex items-center justify-center transition-colors"
                :class="isSeasonChecked(cat, s) ? 'bg-green-600 border-green-600' : 'border-gray-300'">
                <span v-if="isSeasonChecked(cat, s)" class="text-white text-xs">✓</span>
              </span>
              {{ cat }}
            </label>
          </div>
        </div>
        <button @click="saveSeasons" class="btn-primary">💾 保存季节配置</button>
        <p v-if="seasonMsg" class="text-sm text-center mt-3" :class="seasonOk ? 'text-green-600' : 'text-red-500'">{{ seasonMsg }}</p>
      </div>
    </main>

    <!-- ========== 新增种类确认弹窗 ========== -->
    <div v-if="showNewCategoryConfirm" class="fixed inset-0 z-50 flex items-center justify-center" style="background:rgba(0,0,0,0.3)">
      <div class="bg-white rounded-2xl shadow-2xl p-6 w-80 text-center">
        <span class="text-4xl">🌱</span>
        <h4 class="text-lg font-bold text-gray-800 mt-2">确认新增种类</h4>
        <p class="text-sm text-gray-500 mt-1">该操作将新增种类：<strong>{{ pendingNewCategory }}</strong></p>
        <div class="flex gap-3 mt-4">
          <button @click="cancelNewCategory" class="flex-1 py-2 rounded-lg border border-gray-200 text-gray-500 text-sm">取消</button>
          <button @click="confirmNewCategory" class="flex-1 py-2 rounded-lg bg-green-500 text-white text-sm font-medium hover:bg-green-600">确认新增</button>
        </div>
      </div>
    </div>

    <!-- ========== 删除确认弹窗 ========== -->
    <div v-if="deleting" class="fixed inset-0 z-50 flex items-center justify-center" style="background:rgba(0,0,0,0.3)">
      <div class="bg-white rounded-2xl shadow-2xl p-6 w-80 text-center">
        <span class="text-4xl">⚠️</span>
        <h4 class="text-lg font-bold text-gray-800 mt-2">确认删除</h4>
        <p class="text-sm text-gray-500 mt-1">确定删除 <strong>{{ deleting.name }}</strong> 吗？此操作不可撤销。</p>
        <div class="flex gap-3 mt-4">
          <button @click="deleting = null" class="flex-1 py-2 rounded-lg border border-gray-200 text-gray-500 text-sm">取消</button>
          <button @click="doDelete" class="flex-1 py-2 rounded-lg bg-red-500 text-white text-sm font-medium hover:bg-red-600">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue';
import { api } from '@/services/api';
import AMapLoader from '@amap/amap-jsapi-loader';

const SPECIES_EMOJI: Record<string, string> = { '银杏': '🌿', '香樟': '🌳', '樱花': '🌸' };
const emoji = (c: string) => SPECIES_EMOJI[c] || '🌱';

const tabs = [
  { key: 'list' as const, label: '📋 树木目录' },
  { key: 'add' as const, label: '➕ 新增树木' },
  { key: 'season' as const, label: '🌿 季节管理' },
];

const activeTab = ref<'list' | 'add' | 'edit' | 'season'>('list');
const token = ref(localStorage.getItem('admin_token') || '');
const username = ref(''), password = ref(''), error = ref('');
const message = ref(''), ok = ref(true);
const file = ref<File | null>(null), submitting = ref(false);
const plants = ref<any[]>([]);
const categories = ref<string[]>([]);
const editing = ref<any>(null);
const deleting = ref<any>(null);
const showMapPicker = ref(false);
const isMobile = ref(window.innerWidth < 768);
const descriptionDirty = ref(false);
const showNewCategoryConfirm = ref(false);
const pendingNewCategory = ref('');

// 监听窗口宽度
if (typeof window !== 'undefined') {
  window.addEventListener('resize', () => { isMobile.value = window.innerWidth < 768; });
}

const toggleMapPicker = () => {
  showMapPicker.value = !showMapPicker.value;
};

const form = ref({ category: '', description: '', locationName: '', longitude: 0, latitude: 0 });

let pickerMap: any = null;
let pickerMarker: any = null;

const load = async () => {
  try {
    const { data } = await api.getPlants();
    plants.value = data.data || [];
    categories.value = [...new Set(plants.value.map((p: any) => p.category))] as string[];
  } catch {}
};

// ---- 类型变化 → 自动匹配简介 ----
const onCategoryChange = () => {
  if (descriptionDirty.value) return;
  const match = plants.value.find((p: any) => p.category === form.value.category && p.description);
  if (match) form.value.description = match.description;
};

// ---- 切换标签 ----
const switchTab = (key: 'list' | 'add' | 'edit' | 'season') => {
  if (key === 'add') cancelEdit();
  if (key === 'season') loadSeasons();
  activeTab.value = key;
};

// ---- 编辑 ----
const startEdit = (p: any) => {
  editing.value = p;
  form.value = {
    category: p.category,
    description: p.description || '',
    locationName: p.locationName || '',
    longitude: p.longitude || 116.313,
    latitude: p.latitude || 39.971,
  };
  descriptionDirty.value = true;
  activeTab.value = 'edit';
  showMapPicker.value = false;
};

const cancelEdit = () => {
  editing.value = null;
  form.value = { category: '', description: '', locationName: '', longitude: 0, latitude: 0 };
  file.value = null;
  descriptionDirty.value = false;
  showMapPicker.value = false;
  activeTab.value = 'list';
};

// ---- 删除 ----
const confirmDelete = (p: any) => { deleting.value = p; };

const doDelete = async () => {
  if (!deleting.value) return;
  try {
    await api.deletePlant(deleting.value.id);
    deleting.value = null;
    load();
  } catch { alert('删除失败'); }
};

// ---- 地图选点 ----
watch(showMapPicker, async (show) => {
  if (!show) return;
  await nextTick();
  const AMap = await AMapLoader.load({ key: import.meta.env.VITE_AMAP_KEY, version: '2.0' });
  const lng = form.value.longitude || 116.313;
  const lat = form.value.latitude || 39.971;

  // 卫星图 + 路网叠加（最清晰的组合）
  const satellite = new AMap.TileLayer.Satellite();
  const roadNet = new AMap.TileLayer.RoadNet();

  pickerMap = new AMap.Map('picker-map', {
    zoom: 17, center: [lng, lat], resizeEnable: true,
    layers: [satellite, roadNet],
  });

  pickerMarker = new AMap.Marker({
    position: [lng, lat],
    draggable: true,
    map: pickerMap,
  });

  pickerMarker.on('dragend', () => {
    const pos = pickerMarker.getPosition();
    form.value.longitude = Math.round(pos.getLng() * 1000000) / 1000000;
    form.value.latitude = Math.round(pos.getLat() * 1000000) / 1000000;
  });

  pickerMap.on('click', (e: any) => {
    pickerMarker.setPosition(e.lnglat);
    form.value.longitude = Math.round(e.lnglat.getLng() * 1000000) / 1000000;
    form.value.latitude = Math.round(e.lnglat.getLat() * 1000000) / 1000000;
  });
});

watch(() => [form.value.longitude, form.value.latitude], ([lng, lat]) => {
  if (pickerMarker && lng && lat) {
    pickerMarker.setPosition([lng, lat]);
  }
});

// ---- 提交 ----
const submitPlant = async () => {
  if (!form.value.category || !form.value.locationName) {
    message.value = '请填写类型和位置'; ok.value = false; return;
  }
  // 检测是否为新增种类
  if (!editing.value && !categories.value.includes(form.value.category)) {
    pendingNewCategory.value = form.value.category;
    showNewCategoryConfirm.value = true;
    return;
  }
  await doSubmit();
};

const confirmNewCategory = () => {
  showNewCategoryConfirm.value = false;
  doSubmit();
};

const cancelNewCategory = () => {
  showNewCategoryConfirm.value = false;
  pendingNewCategory.value = '';
};

const doSubmit = async () => {
  submitting.value = true;
  try {
    const fd = new FormData();
    fd.append('category', form.value.category);
    fd.append('description', form.value.description);
    fd.append('name', form.value.locationName);
    fd.append('longitude', String(form.value.longitude));
    fd.append('latitude', String(form.value.latitude));
    if (file.value) fd.append('file', file.value);

    if (editing.value) {
      await api.updatePlant(editing.value.id, fd);
    } else {
      await api.createPlant(fd);
    }
    message.value = '操作成功！'; ok.value = true;
    cancelEdit();
    load();
    activeTab.value = 'list';
  } catch { message.value = '操作失败，请重试'; ok.value = false; }
  finally { submitting.value = false; }
};

// ---- 登录 ----
const doLogin = async () => {
  if (!username.value || !password.value) { error.value = '请输入'; return; }
  try {
    const { data } = await api.login(username.value, password.value);
    token.value = data.data;
    localStorage.setItem('admin_token', data.data);
    error.value = '';
    load();
  } catch { error.value = '用户名或密码错误'; }
};
const logout = () => { localStorage.removeItem('admin_token'); token.value = ''; };

const onFileChange = (e: Event) => { file.value = (e.target as HTMLInputElement).files?.[0] || null; };

// ---- 季节管理 ----
const seasonLabels = ['🌸 春', '☀️ 夏', '🍂 秋', '❄️ 冬'];
const seasonKeys = ['春', '夏', '秋', '冬'];
const seasonMap = ref<Record<string, string[]>>({ '🌸 春': [], '☀️ 夏': [], '🍂 秋': [], '❄️ 冬': [] });
const seasonMsg = ref(''), seasonOk = ref(true);

const loadSeasons = async () => {
  try {
    const { data } = await api.getSeasons();
    const list = data.data || [];
    for (const s of seasonLabels) seasonMap.value[s] = [];
    for (const item of list) {
      const idx = seasonKeys.indexOf(item.season);
      if (idx >= 0) seasonMap.value[seasonLabels[idx]].push(item.category);
    }
  } catch {}
};

const isSeasonChecked = (cat: string, s: string) =>
  seasonMap.value[s]?.includes(cat) || false;

const toggleSeasonCat = (cat: string, s: string) => {
  const arr = seasonMap.value[s];
  const idx = arr.indexOf(cat);
  if (idx >= 0) arr.splice(idx, 1);
  else arr.push(cat);
};

const saveSeasons = async () => {
  const mappings: { category: string; season: string }[] = [];
  for (let i = 0; i < seasonLabels.length; i++) {
    for (const cat of seasonMap.value[seasonLabels[i]]) {
      mappings.push({ category: cat, season: seasonKeys[i] });
    }
  }
  try {
    await api.saveSeasons(mappings);
    seasonMsg.value = '保存成功'; seasonOk.value = true;
  } catch {
    seasonMsg.value = '保存失败'; seasonOk.value = false;
  }
};

// 初始化
onMounted(() => { if (token.value) { load(); loadSeasons(); } });
</script>
