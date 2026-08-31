<template>
  <div class="admin-page">
    <!-- ========== Sidebar ========== -->
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <router-link to="/" class="sidebar-home-link">
          <span>←</span> <span>返回地图</span>
        </router-link>
        <h2 class="sidebar-title">🌳 管理后台</h2>
      </div>

      <nav class="sidebar-nav">
        <button
          v-for="t in tabs" :key="t.key"
          @click="switchTab(t.key)"
          class="sidebar-nav-item"
          :class="{ active: activeTab === t.key }"
        >
          <span class="sidebar-nav-icon">{{ t.icon }}</span>
          <span>{{ t.label }}</span>
        </button>
      </nav>

      <div v-if="token" class="sidebar-footer">
        <button @click="logout" class="sidebar-logout-btn">退出登录</button>
      </div>
    </aside>

    <!-- ========== Main ========== -->
    <main class="admin-main">
      <!-- ─── Login ─── -->
      <div v-if="!token" class="admin-login-center">
        <div class="login-card">
          <div class="login-header">
            <span class="login-badge">中国人民大学</span>
            <h2 class="login-title">植物地图管理</h2>
            <p class="login-hint">请输入管理员凭据</p>
          </div>
          <div class="login-form">
            <input v-model="username" class="input" placeholder="用户名" @keyup.enter="doLogin" />
            <input v-model="password" class="input" type="password" placeholder="密码" @keyup.enter="doLogin" />
            <p v-if="error" class="login-error">{{ error }}</p>
            <button @click="doLogin" class="btn btn-primary login-submit">登录</button>
          </div>
        </div>
      </div>

      <!-- ─── Plant list ─── -->
      <div v-if="token && activeTab === 'list'" class="admin-section">
        <div class="section-head">
          <div>
            <h3 class="section-title">树木目录</h3>
            <p class="section-desc">管理校园内所有植物记录</p>
          </div>
          <span class="section-badge">{{ plants.length }} 棵</span>
        </div>

        <div class="plant-list">
          <div v-for="p in plants" :key="p.id" class="plant-row">
            <span class="plant-row-emoji">{{ plantEmoji(p.category) }}</span>
            <div class="plant-row-body">
              <p class="plant-row-name">{{ p.name }}</p>
              <p class="plant-row-meta">{{ p.category }} · {{ p.locationName }}</p>
            </div>
            <div class="plant-row-actions">
              <button @click="startEdit(p)" class="btn btn-ghost btn-sm">修改</button>
              <button @click="confirmDelete(p)" class="btn btn-danger btn-sm">删除</button>
            </div>
          </div>
          <p v-if="!plants.length" class="empty-hint">
            暂无数据 — 前往「新增树木」添加第一棵植物
          </p>
        </div>
      </div>

      <!-- ─── Add / Edit plant ─── -->
      <div v-if="token && (activeTab === 'add' || activeTab === 'edit')" class="admin-section">
        <div class="section-head">
          <div>
            <h3 class="section-title">{{ editing ? '修改树木信息' : '新增树木' }}</h3>
            <p class="section-desc">
              {{ editing ? `正在编辑：${editing.name}` : '添加一棵新植物到校园地图' }}
            </p>
          </div>
          <button v-if="editing" @click="cancelEdit" class="btn btn-ghost btn-sm">取消编辑</button>
        </div>

        <!-- Card 1: Species info -->
        <div class="card form-card">
          <h4 class="form-card-title">物种信息</h4>
          <div class="form-row">
            <div class="form-field">
              <label class="field-label">类型（物种名）</label>
              <input v-model="form.category" class="input" placeholder="如 银杏、樱花" @input="onCategoryChange" list="category-list" />
              <datalist id="category-list">
                <option v-for="c in categories" :key="c" :value="c" />
              </datalist>
            </div>
            <div class="form-field">
              <label class="field-label">
                植物简介
                <span class="field-label-hint">（填写类型后自动匹配已有描述）</span>
              </label>
              <textarea v-model="form.description" class="input" rows="4" placeholder="描述该植物的特征、观赏期、识别要点等" @input="descriptionDirty = true"></textarea>
            </div>
          </div>
        </div>

        <!-- Card 2: Location -->
        <div class="card form-card">
          <h4 class="form-card-title">位置信息</h4>
          <div class="form-row">
            <div class="form-field">
              <label class="field-label">位置描述</label>
              <input v-model="form.locationName" class="input" placeholder="如图书馆东侧、一勺池旁" />
            </div>
            <div class="form-field">
              <label class="field-label">地理坐标</label>
              <div class="coord-row">
                <div class="coord-field">
                  <span class="coord-label">经度</span>
                  <input v-model.number="form.longitude" type="number" step="0.000001" class="input input-sm mono" />
                </div>
                <span class="coord-sep">,</span>
                <div class="coord-field">
                  <span class="coord-label">纬度</span>
                  <input v-model.number="form.latitude" type="number" step="0.000001" class="input input-sm mono" />
                </div>
                <button @click="toggleMapPicker" class="btn btn-ghost btn-sm coord-map-btn">
                  {{ showMapPicker ? '收起地图' : '地图选点' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Card 3: Map picker (togglable, full width) -->
        <div v-if="showMapPicker" class="card form-card">
          <h4 class="form-card-title">
            在地图上点击或拖动标记选择位置
          </h4>
          <div id="picker-map" class="picker-map"></div>
        </div>

        <!-- Card 4: Photo upload -->
        <div class="card form-card">
          <h4 class="form-card-title">植物照片</h4>
          <div class="form-row">
            <div class="form-field">
              <label class="upload-area" :class="{ 'has-file': file }">
                <input type="file" accept="image/*" hidden @change="onFileChange" />
                <span class="upload-icon">{{ file ? '✅' : '📷' }}</span>
                <span class="upload-hint">{{ file ? `已选择：${file.name}` : '点击此处上传照片' }}</span>
                <span v-if="!file" class="upload-sub-hint">支持 JPG、PNG，建议 16:9 横版照片</span>
              </label>
            </div>
          </div>
        </div>

        <!-- Submit -->
        <div class="form-submit-row">
          <p v-if="message" class="form-feedback" :class="{ error: !ok }">{{ message }}</p>
          <button @click="submitPlant" class="btn btn-primary form-submit-btn" :disabled="submitting">
            {{ submitting ? '提交中…' : (editing ? '保存修改' : '确认新增') }}
          </button>
        </div>
      </div>

      <!-- ─── Season ─── -->
      <div v-if="token && activeTab === 'season'" class="admin-section">
        <div class="section-head">
          <div>
            <h3 class="section-title">季节浏览配置</h3>
            <p class="section-desc">设定每个季节在地图上展示的植物类型</p>
          </div>
        </div>

        <div class="season-grid">
          <div v-for="s in seasonLabels" :key="s" class="card season-card">
            <h4 class="season-card-title">{{ s }}</h4>
            <div class="season-card-options">
              <label
                v-for="cat in categories" :key="cat"
                class="season-check-item"
                @click="toggleSeasonCat(cat, s)"
              >
                <span class="check-square" :class="{ active: isSeasonChecked(cat, s) }"></span>
                <span class="season-check-label">{{ cat }}</span>
              </label>
            </div>
          </div>
        </div>

        <div class="form-submit-row">
          <p v-if="seasonMsg" class="form-feedback" :class="{ error: !seasonOk }">{{ seasonMsg }}</p>
          <button @click="saveSeasons" class="btn btn-primary form-submit-btn">保存季节配置</button>
        </div>
      </div>
    </main>

    <!-- ========== New category confirm modal ========== -->
    <div v-if="showNewCategoryConfirm" class="overlay-backdrop" @click.self="cancelNewCategory">
      <div class="modal-dialog">
        <span class="modal-icon">🌱</span>
        <h4 class="modal-title">新种类确认</h4>
        <p class="modal-text">系统中尚未收录 <strong>{{ pendingNewCategory }}</strong>，确认要新增吗？</p>
        <div class="modal-actions">
          <button @click="cancelNewCategory" class="btn btn-ghost modal-btn">取消</button>
          <button @click="confirmNewCategory" class="btn btn-primary modal-btn">确认</button>
        </div>
      </div>
    </div>

    <!-- ========== Delete confirm modal ========== -->
    <div v-if="deleting" class="overlay-backdrop" @click.self="deleting = null">
      <div class="modal-dialog">
        <span class="modal-icon modal-icon-warn">⚠️</span>
        <h4 class="modal-title">删除确认</h4>
        <p class="modal-text">确定删除 <strong>{{ deleting.name }}</strong> 吗？此操作无法撤销。</p>
        <div class="modal-actions">
          <button @click="deleting = null" class="btn btn-ghost modal-btn">取消</button>
          <button @click="doDelete" class="btn btn-danger modal-btn">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue';
import { api } from '@/services/api';
import AMapLoader from '@amap/amap-jsapi-loader';

const PLANT_EMOJI: Record<string, string> = {
  '银杏': '🍂', '香樟': '🌳', '桂花': '🌼', '樱花': '🌸', '悬铃木': '🍁'
};
const plantEmoji = (c: string) => PLANT_EMOJI[c] || '🌱';

const tabs = [
  { key: 'list' as const, label: '树木目录', icon: '📋' },
  { key: 'add'  as const, label: '新增树木', icon: '➕' },
  { key: 'season' as const, label: '季节管理', icon: '🌿' },
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
const descriptionDirty = ref(false);
const showNewCategoryConfirm = ref(false);
const pendingNewCategory = ref('');

const toggleMapPicker = () => { showMapPicker.value = !showMapPicker.value; };

const form = ref({
  category: '', description: '', locationName: '',
  longitude: 0, latitude: 0,
});

let pickerMap: any = null;
let pickerMarker: any = null;

const load = async () => {
  try {
    const { data } = await api.getPlants();
    plants.value = data.data || [];
    categories.value = [...new Set(plants.value.map((p: any) => p.category))] as string[];
  } catch { /* */ }
};

const onCategoryChange = () => {
  if (descriptionDirty.value) return;
  const match = plants.value.find(
    (p: any) => p.category === form.value.category && p.description
  );
  if (match) form.value.description = match.description;
};

const switchTab = (key: 'list' | 'add' | 'edit' | 'season') => {
  if (key === 'add') { cancelEdit(); showMapPicker.value = false; }
  if (key === 'season') loadSeasons();
  activeTab.value = key;
};

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
  showMapPicker.value = false;
  activeTab.value = 'edit';
};

const cancelEdit = () => {
  editing.value = null;
  form.value = { category: '', description: '', locationName: '', longitude: 0, latitude: 0 };
  file.value = null;
  descriptionDirty.value = false;
  showMapPicker.value = false;
  activeTab.value = 'list';
};

const confirmDelete = (p: any) => { deleting.value = p; };

const doDelete = async () => {
  if (!deleting.value) return;
  try {
    await api.deletePlant(deleting.value.id);
    deleting.value = null;
    load();
  } catch { alert('删除失败'); }
};

// Map picker
watch(showMapPicker, async (show) => {
  if (!show) return;
  await nextTick();
  const AMap = await AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY, version: '2.0',
  });
  const lng = form.value.longitude || 116.313;
  const lat = form.value.latitude || 39.971;

  const satellite = new AMap.TileLayer.Satellite();
  const roadNet = new AMap.TileLayer.RoadNet();

  pickerMap = new AMap.Map('picker-map', {
    zoom: 17, center: [lng, lat], resizeEnable: true,
    layers: [satellite, roadNet],
  });

  pickerMarker = new AMap.Marker({
    position: [lng, lat], draggable: true, map: pickerMap,
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
  if (pickerMarker && lng && lat) pickerMarker.setPosition([lng, lat]);
});

// Submit
const submitPlant = async () => {
  if (!form.value.category || !form.value.locationName) {
    message.value = '请填写物种类型和位置描述'; ok.value = false; return;
  }
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
    message.value = editing.value ? '修改成功' : '新增成功'; ok.value = true;
    cancelEdit(); load();
    activeTab.value = 'list';
  } catch {
    message.value = '操作失败，请重试'; ok.value = false;
  } finally { submitting.value = false; }
};

// Login
const doLogin = async () => {
  if (!username.value || !password.value) { error.value = '请输入用户名和密码'; return; }
  try {
    const { data } = await api.login(username.value, password.value);
    token.value = data.data;
    localStorage.setItem('admin_token', data.data);
    error.value = ''; load();
  } catch { error.value = '用户名或密码错误'; }
};

const logout = () => {
  localStorage.removeItem('admin_token');
  token.value = '';
};

const onFileChange = (e: Event) => {
  file.value = (e.target as HTMLInputElement).files?.[0] || null;
};

// Season
const seasonLabels = ['🌸 春季', '☀️ 夏季', '🍂 秋季', '❄️ 冬季'];
const seasonKeys = ['春', '夏', '秋', '冬'];
const seasonMap = ref<Record<string, string[]>>(
  Object.fromEntries(seasonLabels.map(s => [s, [] as string[]]))
);
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
  } catch { /* */ }
};

const isSeasonChecked = (cat: string, s: string) =>
  seasonMap.value[s]?.includes(cat) || false;

const toggleSeasonCat = (cat: string, s: string) => {
  const arr = seasonMap.value[s];
  const idx = arr.indexOf(cat);
  if (idx >= 0) arr.splice(idx, 1); else arr.push(cat);
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

onMounted(() => {
  if (token.value) { load(); loadSeasons(); }
});
</script>

<style scoped>
/* ============================================
   Admin Page — "The Curator's Desk"
   Herbarium management interface
   ============================================ */

/* ─── Layout ─── */
.admin-page {
  display: flex; height: 100vh; overflow: hidden;
  background: #F4EFE6;
}

/* ─── Sidebar ─── */
.admin-sidebar {
  width: 13rem; flex-shrink: 0;
  background: #FDFCF9;
  border-right: 1px solid #EBE6DD;
  display: flex; flex-direction: column;
}
.sidebar-header {
  padding: 1.25rem 1rem 1rem;
  border-bottom: 1px solid #EBE6DD;
}
.sidebar-home-link {
  display: inline-flex; align-items: center; gap: 0.35rem;
  font-size: 0.75rem; color: #8B8581;
  text-decoration: none; transition: color 0.15s;
}
.sidebar-home-link:hover { color: var(--color-accent); }
.sidebar-title {
  font-family: var(--font-display);
  font-size: 1.0625rem; font-weight: 700;
  margin-top: 0.75rem; color: var(--color-text);
}
.sidebar-nav {
  flex: 1; padding: 0.6rem; display: flex; flex-direction: column; gap: 1px;
}
.sidebar-nav-item {
  width: 100%; text-align: left;
  padding: 0.55rem 0.75rem; border: none; border-radius: 8px;
  background: none; cursor: pointer;
  font-family: var(--font-body); font-size: 0.8125rem;
  font-weight: 500; color: #6E6A66;
  display: flex; align-items: center; gap: 0.5rem;
  transition: all 0.12s;
}
.sidebar-nav-icon { font-size: 0.9rem; width: 1.2rem; text-align: center; }
.sidebar-nav-item:hover { background: #ECF4EE; color: var(--color-accent); }
.sidebar-nav-item.active {
  background: #ECF4EE; color: var(--color-accent); font-weight: 600;
}
.sidebar-footer {
  padding: 0.75rem; border-top: 1px solid #EBE6DD;
}
.sidebar-logout-btn {
  width: 100%; padding: 0.45rem; border: none; border-radius: 8px;
  background: none; cursor: pointer;
  font-family: var(--font-body); font-size: 0.75rem;
  color: #9D9A96;
  transition: all 0.12s;
}
.sidebar-logout-btn:hover { background: #FEF2F2; color: var(--color-danger); }

/* ─── Main scroll area ─── */
.admin-main {
  flex: 1; overflow-y: auto; min-width: 0;
}

/* ─── Login ─── */
.admin-login-center {
  display: flex; align-items: center; justify-content: center;
  min-height: 100%;
  padding: 2rem;
}
.login-card {
  width: 22rem; max-width: 100%;
  background: #FDFCF9;
  border: 1px solid #EBE6DD;
  border-radius: 14px;
  padding: 2.5rem 2rem; text-align: center;
  box-shadow: 0 2px 16px rgba(0,0,0,0.05);
}
.login-badge {
  display: inline-block; padding: 0.2rem 0.7rem; border-radius: 100px;
  background: #ECF4EE; color: var(--color-accent);
  font-size: 0.7rem; font-weight: 600;
  margin-bottom: 0.75rem;
}
.login-title {
  font-family: var(--font-display); font-size: 1.3rem; font-weight: 700;
}
.login-hint {
  font-size: 0.8125rem; color: #9D9A96;
  margin-top: 0.4rem;
}
.login-form {
  display: flex; flex-direction: column; gap: 0.75rem;
  margin-top: 1.5rem;
}
.login-submit { width: 100%; padding: 0.65rem; }
.login-error {
  font-size: 0.75rem; color: var(--color-danger); text-align: center;
}

/* ─── Section header ─── */
.admin-section {
  padding: 2rem;
  max-width: 48rem;
}
.section-head {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 1.5rem;
}
.section-title {
  font-family: var(--font-display);
  font-size: 1.25rem; font-weight: 700; color: var(--color-text);
}
.section-desc {
  font-size: 0.8125rem; color: #9D9A96;
  margin-top: 0.25rem;
}
.section-badge {
  font-size: 0.8125rem; font-weight: 600; color: var(--color-accent);
  background: #ECF4EE; padding: 0.25rem 0.7rem; border-radius: 100px;
  flex-shrink: 0;
}

/* ─── Plant list ─── */
.plant-list {
  display: flex; flex-direction: column; gap: 0.5rem;
}
.plant-row {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: #FDFCF9; border: 1px solid #EBE6DD;
  border-radius: 10px;
}
.plant-row-emoji { font-size: 1.5rem; flex-shrink: 0; }
.plant-row-body { flex: 1; min-width: 0; }
.plant-row-name {
  font-size: 0.875rem; font-weight: 600; color: var(--color-text);
}
.plant-row-meta {
  font-size: 0.75rem; color: #9D9A96; margin-top: 0.1rem;
}
.plant-row-actions { display: flex; gap: 0.4rem; flex-shrink: 0; }
.btn-sm { padding: 0.35rem 0.65rem; font-size: 0.75rem; }
.empty-hint {
  text-align: center; color: #9D9A96;
  font-size: 0.8125rem; padding: 2.5rem 0;
}

/* ─── Form cards ─── */
.form-card {
  background: #FDFCF9;
  border: 1px solid #EBE6DD;
  border-radius: 12px;
  padding: 1.25rem;
  margin-bottom: 1rem;
}
.form-card-title {
  font-family: var(--font-display);
  font-size: 0.875rem; font-weight: 700;
  color: var(--color-text);
  margin-bottom: 1rem;
  padding-bottom: 0.6rem;
  border-bottom: 1px solid #F0EDE8;
}
.form-row {
  display: flex; flex-direction: column; gap: 1rem;
}

/* Field */
.form-field { display: flex; flex-direction: column; }
.field-label {
  font-size: 0.75rem; font-weight: 600;
  color: #6E6A66;
  margin-bottom: 0.4rem;
}
.field-label-hint {
  font-weight: 400; color: #B8B3AD; margin-left: 0.3rem;
}

/* Coord row */
.coord-row {
  display: flex; align-items: flex-end; gap: 0.5rem;
  flex-wrap: wrap;
}
.coord-field { flex: 1; min-width: 6rem; display: flex; flex-direction: column; }
.coord-label {
  font-size: 0.7rem; color: #9D9A96; margin-bottom: 0.2rem;
}
.coord-sep {
  font-size: 0.875rem; color: #B8B3AD;
  padding-bottom: 0.45rem; flex-shrink: 0;
}
.coord-map-btn { flex-shrink: 0; }
.input-sm { padding: 0.4rem 0.6rem; font-size: 0.75rem; }

/* Map picker */
.picker-map {
  width: 100%; height: 16rem;
  border-radius: 8px;
  background: #F4EFE6;
}

/* Upload area */
.upload-area {
  display: flex; flex-direction: column; align-items: center;
  padding: 1.75rem 1.5rem;
  border: 2px dashed #D9D4CC;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}
.upload-area:hover { border-color: var(--color-accent); background: #FAF8F4; }
.upload-area.has-file { border-color: var(--color-accent); border-style: solid; }
.upload-icon { font-size: 2rem; display: block; }
.upload-hint {
  font-size: 0.875rem; color: #6E6A66;
  margin-top: 0.5rem;
}
.upload-sub-hint {
  font-size: 0.75rem; color: #B8B3AD;
  margin-top: 0.25rem;
}

/* Submit row */
.form-submit-row {
  display: flex; align-items: center; gap: 1rem;
  margin-top: 0.25rem;
}
.form-feedback {
  font-size: 0.8125rem; color: var(--color-accent);
  flex: 1;
}
.form-feedback.error { color: var(--color-danger); }
.form-submit-btn { padding: 0.65rem 2rem; }

/* ─── Season ─── */
.season-grid {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem; margin-bottom: 1.5rem;
}
.season-card {
  background: #FDFCF9;
  border: 1px solid #EBE6DD;
  border-radius: 12px;
  padding: 1rem;
}
.season-card-title {
  font-family: var(--font-display);
  font-size: 0.875rem; font-weight: 700;
  margin-bottom: 0.75rem; padding-bottom: 0.5rem;
  border-bottom: 1px solid #F0EDE8;
}
.season-card-options {
  display: flex; flex-direction: column; gap: 0.3rem;
}
.season-check-item {
  display: flex; align-items: center; gap: 0.5rem;
  cursor: pointer; padding: 0.25rem 0; user-select: none;
}
.season-check-label {
  font-size: 0.8125rem; color: #6E6A66;
  transition: color 0.12s;
}
.season-check-item:hover .season-check-label { color: var(--color-accent); }

/* ─── Modal ─── */
.modal-dialog {
  width: 20rem; max-width: calc(100vw - 2rem);
  background: #FDFCF9; border-radius: 14px;
  padding: 2rem; text-align: center;
  box-shadow: 0 12px 40px rgba(0,0,0,0.12);
  animation: modalSlide 0.2s ease;
}
@keyframes modalSlide { from { opacity: 0; transform: translateY(0.5rem); } to { opacity: 1; transform: translateY(0); } }
.modal-icon { font-size: 2.5rem; display: block; margin-bottom: 0.75rem; }
.modal-icon-warn { font-size: 2.5rem; }
.modal-title {
  font-family: var(--font-display);
  font-size: 1.125rem; font-weight: 700;
  margin-bottom: 0.5rem;
}
.modal-text {
  font-size: 0.8125rem; color: #6E6A66;
  line-height: 1.6; margin-bottom: 1.25rem;
}
.modal-actions { display: flex; gap: 0.75rem; }
.modal-btn { flex: 1; }

/* ─── Mobile ─── */
@media (max-width: 768px) {
  .admin-page { flex-direction: column; }
  .admin-sidebar {
    width: 100%; flex-direction: row; align-items: center;
    border-right: none; border-bottom: 1px solid #EBE6DD;
    padding: 0 0.5rem;
  }
  .sidebar-header { display: none; }
  .sidebar-nav { flex-direction: row; padding: 0.4rem; gap: 0; }
  .sidebar-nav-item { font-size: 0.7rem; padding: 0.35rem 0.5rem; }
  .sidebar-footer { display: none; }
  .admin-section { padding: 1rem; }
  .coord-row { flex-direction: column; align-items: stretch; }
  .coord-sep { display: none; }
  .season-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
