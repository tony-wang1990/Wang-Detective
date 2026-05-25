<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Activity, Database, FileJson, ServerCog, ShieldCheck, Terminal } from 'lucide-vue-next';
import { apiGet, getHealth, type HealthData } from '../api/http';

defineProps<{
  mode?: 'legacy';
}>();

type DiagItem = {
  key?: string;
  name?: string;
  status?: string;
  message?: string;
  detail?: string;
};

const health = ref<HealthData>({});
const checks = ref<DiagItem[]>([]);
const raw = ref('');
const loading = ref(false);
const displayVersion = computed(() => health.value.version || localStorage.getItem('currentVersion') || 'main');

async function refresh() {
  loading.value = true;
  health.value = await getHealth().catch(() => ({}));
  try {
    const res = await apiGet<Record<string, unknown>>('/v1/system/diagnostics');
    checks.value = ((res.data?.checks || []) as DiagItem[]).slice(0, 8);
    raw.value = JSON.stringify(res.data || res, null, 2);
  } catch (err) {
    raw.value = err instanceof Error ? err.message : '诊断接口读取失败';
  } finally {
    loading.value = false;
  }
}

const diagnosticNames: Record<string, string> = {
  'database-connectivity': '数据库连接',
  'database-file': '数据库文件',
  'key-directory': 'OCI 私钥目录',
  'log-file': '服务日志文件',
  'data-directory': '数据目录',
  'admin-password': '管理员密码',
  'telegram-bot': 'Telegram Bot'
};

const cards = [
  { title: '部署稳定性', desc: '安装脚本、低配 JVM、IPv4 监听、健康检查和持久化目录已统一。', icon: ShieldCheck },
  { title: '系统诊断', desc: '数据库、数据目录、密钥目录、日志、管理员密码、Telegram 和资源状态检查。', icon: Activity },
  { title: '运维终端', desc: 'Web SSH、命令执行、主机资产库、SFTP 文件操作入口。', icon: Terminal },
  { title: '主机资产库', desc: '保存常用主机，敏感凭据加密存储，后续补权限与审计筛选。', icon: Database }
];

onMounted(refresh);
</script>

<template>
  <section class="wd-page">
    <div class="wd-page-title">
      <div>
        <h1>{{ mode === 'legacy' ? '页面迁移中' : '功能中心' }}</h1>
        <p>这一版已经从 iframe 过渡为 Vue 原生路由，后续新能力继续按当前控制台框架扩展。</p>
      </div>
      <div class="wd-actions">
        <button type="button" :disabled="loading" @click="refresh"><Activity :size="16" :class="{ spinning: loading }" />{{ loading ? '刷新中' : '刷新诊断' }}</button>
      </div>
    </div>

    <section class="wd-kpis">
      <article class="wd-kpi">
        <Activity :size="24" />
        <div><span>当前健康状态</span><strong>{{ health.status || 'UNKNOWN' }}</strong></div>
      </article>
      <article class="wd-kpi">
        <ServerCog :size="24" />
        <div><span>运行版本</span><strong>{{ displayVersion }}</strong></div>
      </article>
      <article class="wd-kpi">
        <Database :size="24" />
        <div><span>数据库连接</span><strong>{{ health.databaseConnectivity ? '正常' : '待检查' }}</strong></div>
      </article>
      <article class="wd-kpi">
        <Terminal :size="24" />
        <div><span>运维入口</span><strong>已接入</strong></div>
      </article>
    </section>

    <section class="wd-split">
      <div class="wd-card wd-feature-grid">
        <article v-for="card in cards" :key="card.title">
          <component :is="card.icon" :size="22" />
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
        </article>
      </div>
      <div class="wd-card">
        <header><h2><FileJson :size="17" /> 系统诊断结果</h2></header>
        <div class="wd-health-list">
          <div v-for="item in checks" :key="item.key || item.name">
            <Activity :size="18" />
            <b>{{ diagnosticNames[String(item.key || item.name || '')] || item.name || item.key }}</b>
            <em :class="String(item.status || '').toLowerCase()">{{ item.status || 'INFO' }}</em>
            <small>{{ item.message || item.detail }}</small>
          </div>
        </div>
      </div>
    </section>

    <div class="wd-card wd-log-card">
      <header><h2>接口原始返回</h2></header>
      <pre class="wd-terminal small">{{ raw || '暂无诊断返回' }}</pre>
    </div>
  </section>
</template>
