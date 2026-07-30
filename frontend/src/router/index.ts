import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/HomePage.vue'),
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/AdminPage.vue'),
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/pages/AdminPage.vue'),
  },
  {
    path: '/plant/:id',
    name: 'plant-detail',
    component: () => import('@/pages/PlantDetailPage.vue'),
  },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
