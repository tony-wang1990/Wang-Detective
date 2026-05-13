<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Activity, CalendarDays, Globe2, Server, Zap } from 'lucide-vue-next';
import { apiGet, getHealth, type GlanceData, type HealthData } from '../api/http';

const glance = reactive<GlanceData>({});
const health = ref<HealthData>({});
const refreshedAt = ref('');
const displayVersion = computed(() => health.value.version || localStorage.getItem('currentVersion') || 'main');

const stats = [
  { label: '总 API 数量', key: 'users', icon: Server },
  { label: '开机任务数量', key: 'tasks', icon: Zap },
  { label: '区域数量', key: 'regions', icon: Globe2 },
  { label: '运行天数', key: 'days', icon: CalendarDays }
] as const;

function formatBytes(value?: number) {
  if (!value) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  let size = value;
  let index = 0;
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024;
    index += 1;
  }
  return `${size.toFixed(index ? 1 : 0)} ${units[index]}`;
}

async function refresh() {
  const [glanceResult, healthResult] = await Promise.all([
    apiGet<GlanceData>('/sys/glance').catch(() => null),
    getHealth().catch(() => ({} as HealthData))
  ]);
  if (glanceResult?.success) {
    Object.assign(glance, glanceResult.data || {});
    if (glanceResult.data?.currentVersion) {
      localStorage.setItem('currentVersion', glanceResult.data.currentVersion);
    }
  }
  health.value = healthResult;
  refreshedAt.value = new Date().toLocaleTimeString();
}

onMounted(refresh);
</script>

<template>
  <div class="wd-page">
    <section class="wd-kpis">
      <article v-for="item in stats" :key="item.key" class="wd-kpi">
        <component :is="item.icon" :size="24" />
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ glance[item.key] ?? 0 }}</strong>
        </div>
      </article>
    </section>

    <section class="wd-home-grid">
      <article class="wd-card wd-map">
        <header>
          <h2>资源分布地图</h2>
          <button type="button" @click="refresh">刷新</button>
        </header>
        <div class="wd-map-body">
          <div class="wd-map-pin"></div>
          <span>后续迁移 Leaflet 实时资源地图</span>
        </div>
      </article>

      <article class="wd-card">
        <header>
          <h2>系统诊断</h2>
          <span>{{ refreshedAt || '未刷新' }}</span>
        </header>
        <div class="wd-health-list">
          <div>
            <Activity :size="18" />
            <b>API 网关</b>
            <em>{{ health.status || 'UNKNOWN' }}</em>
          </div>
          <div>
            <Activity :size="18" />
            <b>数据库连接</b>
            <em>{{ health.databaseConnectivity ? '正常' : '待检查' }}</em>
          </div>
          <div>
            <Activity :size="18" />
            <b>内存使用</b>
            <em>{{ formatBytes(health.usedMemoryBytes) }} / {{ formatBytes(health.maxMemoryBytes) }}</em>
          </div>
          <div>
            <Activity :size="18" />
            <b>运行版本</b>
            <em>{{ displayVersion }}</em>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
