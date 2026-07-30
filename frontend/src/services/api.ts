import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
});

client.interceptors.request.use((config) => {
  if (config.url?.startsWith('/admin')) {
    const token = localStorage.getItem('admin_token');
    if (token) config.headers['X-Admin-Token'] = token;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('admin_token');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export const api = {
  client,
  login: (username: string, password: string) =>
    client.post('/auth/login', { username, password }),

  getPlants: (search?: string) =>
    client.get('/plants', { params: search ? { search } : {} }),

  getPlantDetail: (id: string) =>
    client.get(`/plants/${id}`),

  createPlant: (form: FormData) =>
    client.post('/admin/plants', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  uploadImage: (id: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return client.post(`/admin/plants/${id}/images`, form);
  },

  deletePlant: (id: string) =>
    client.delete(`/admin/plants/${id}`),

  updatePlant: (id: string, form: FormData) =>
    client.put(`/admin/plants/${id}`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  getSeasons: () => client.get('/seasons'),

  saveSeasons: (mappings: { category: string; season: string }[]) =>
    client.put('/admin/seasons', mappings),
};
