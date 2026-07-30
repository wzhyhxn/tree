<template>
  <div>
    <input
      type="file"
      accept="image/*"
      :disabled="loading"
      @change="handleUpload"
      class="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-green-50 file:text-green-700 hover:file:bg-green-100"
    />
    <p v-if="loading" class="text-sm text-gray-400 mt-1">上传中...</p>
    <img v-if="imageUrl" :src="imageUrl" class="mt-2 w-64 rounded shadow" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { api } from '@/services/api';
import imageCompression from 'browser-image-compression';

const props = defineProps<{ plantId: number }>();
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
