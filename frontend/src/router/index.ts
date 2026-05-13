import { createRouter, createWebHistory } from 'vue-router';
import DashboardLayout from '../layout/DashboardLayout.vue';
import LoginView from '../views/LoginView.vue';
import HomeView from '../views/HomeView.vue';
import FeatureCenterView from '../views/FeatureCenterView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginView },
    {
      path: '/dashboard',
      component: DashboardLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/dashboard/home' },
        { path: 'home', component: HomeView },
        { path: 'features', component: FeatureCenterView },
        { path: 'ops-terminal', component: FeatureCenterView, props: { mode: 'ops' } },
        { path: ':legacyRoute(.*)*', component: FeatureCenterView, props: { mode: 'legacy' } }
      ]
    }
  ]
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !sessionStorage.getItem('token')) {
    return '/login';
  }
  return true;
});

export default router;
