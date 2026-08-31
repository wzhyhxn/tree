<template>
  <div class="image-upload">
    <label class="upload-zone">
      <input
        type="file"
        accept="image/*"
        :disabled="loading"
        hidden
        @change="handleUpload"
      />
      <span class="upload-zone-icon">{{ loading ? '⏳' : '📷' }}</span>
      <p class="upload-zone-text">{{ loading ? '上传中…' : '点击上传图片' }}</p>
    </label>
    <img v-if="imageUrl" :src="imageUrl" class="image-preview" alt="预览" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { api } from '@/services/api';
import imageCompression from 'browser-image-compression';

const props = defineProps<{ plantId: string }>();
const emit = defineEmits<{ uploaded: [url: string] }>();

const loading = ref(false);
const imageUrl = ref('');

const handleUpload = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;

  loading.value = true;
  try {
    const compressed = await imageCompression(file, {
      maxWidthOrHeight: 1080,
      maxSizeMB: 1,
    });
    const { data } = await api.uploadImage(String(props.plantId), compressed);
    imageUrl.value = data.data;
    emit('uploaded', data.data);
  } catch (err) {
    console.error('上传失败:', err);
    alert('上传失败，请重试');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.image-upload {
  display: flex; flex-direction: column; gap: var(--space-3);
}
.image-preview {
  width: 16rem; border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}
</style>
