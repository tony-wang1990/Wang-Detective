import { createRouter, createWebHistory } from 'vue-router';
import DashboardLayout from '../layout/DashboardLayout.vue';
import LoginView from '../views/LoginView.vue';
import HomeView from '../views/HomeView.vue';
import FeatureCenterView from '../views/FeatureCenterView.vue';
import ResourceListView from '../views/ResourceListView.vue';
import TaskListView from '../views/TaskListView.vue';
import ServiceLogView from '../views/ServiceLogView.vue';
import SystemConfigView from '../views/SystemConfigView.vue';
import AiChatView from '../views/AiChatView.vue';
import OpsTerminalView from '../views/OpsTerminalView.vue';

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
        { path: 'user', component: ResourceListView },
        { path: 'createTask', component: TaskListView },
        { path: 'ociLog', component: ServiceLogView },
        { path: 'sysCfg', component: SystemConfigView },
        { path: 'ai-chat', component: AiChatView },
        { path: 'features', component: FeatureCenterView },
        { path: 'ops-terminal', component: OpsTerminalView },
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
