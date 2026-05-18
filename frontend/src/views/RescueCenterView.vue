<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { LifeBuoy, RefreshCw, Server, ShieldAlert, Terminal } from 'lucide-vue-next';
import { apiGet, notifyGlobal } from '../api/http';

type RescueItem = {
  title?: string;
  description?: string;
};

type LocalScript = {
  name?: string;
  description?: string;
  exists?: boolean;
};

type RescueOverview = {
  version?: string;
  appDir?: string;
  warning?: string;
  lightRescue?: RescueItem[];
  bootVolumeRescue?: RescueItem[];
  netbootXyz?: RescueItem[];
  localScripts?: LocalScript[];
};

const loading = ref(false);
const error = ref('');
const overview = ref<RescueOverview>({});
const lightScript = ref('');
const netbootScript = ref('');

async function loadOverview() {
  loading.value = true;
  error.value = '';
  try {
    const [overviewRsp, lightRsp, netbootRsp] = await Promise.all([
      apiGet<RescueOverview>('/v1/rescue/overview'),
      apiGet<string>('/v1/rescue/light-script'),
      apiGet<string>('/v1/rescue/netboot-script?mode=ipxe')
    ]);
    overview.value = overviewRsp.data || {};
    lightScript.value = lightRsp.data || '';
    netbootScript.value = netbootRsp.data || '';
  } catch (err) {
    error.value = err instanceof Error ? err.message : '救援中心加载失败';
    notifyGlobal(error.value, 'error');
  } finally {
    loading.value = false;
  }
}

async function copy(text: string) {
  if (!text) return;
  await navigator.clipboard.writeText(text);
  notifyGlobal('脚本已复制', 'success');
}

onMounted(loadOverview);
</script>

<template>
  <section class="wd-page">
    <div class="wd-page-title">
      <div>
        <h1>救援中心</h1>
        <p>面向 OCI 实例失联、SSH 异常、boot volume 修复和 netboot.xyz 实验引导的安全向导。</p>
      </div>
      <div class="wd-actions">
        <button type="button" :disabled="loading" @click="loadOverview">
          <RefreshCw :size="16" />{{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <p v-if="error" class="wd-error-line">{{ error }}</p>
    <p v-if="overview.warning" class="wd-notice">{{ overview.warning }}</p>

    <section class="wd-log-summary">
      <article class="wd-card wd-stat-card">
        <span>当前版本</span>
        <strong>{{ overview.version || '-' }}</strong>
      </article>
      <article class="wd-card wd-stat-card">
        <span>应用目录</span>
        <strong>{{ overview.appDir || '-' }}</strong>
      </article>
      <article class="wd-card wd-stat-card">
        <span>救援脚本</span>
        <strong>{{ overview.localScripts?.filter((item) => item.exists).length || 0 }}</strong>
      </article>
      <article class="wd-card wd-stat-card">
        <span>安全边界</span>
        <strong>人工确认</strong>
      </article>
    </section>

    <section class="wd-feature-grid">
      <article class="wd-card">
        <header><h2><LifeBuoy :size="17" /> 轻量自救</h2></header>
        <ul class="wd-check-list">
          <li v-for="item in overview.lightRescue || []" :key="item.title">
            <strong>{{ item.title }}</strong>
            <span>{{ item.description }}</span>
          </li>
        </ul>
      </article>

      <article class="wd-card">
        <header><h2><Server :size="17" /> Boot Volume 拆卷救援</h2></header>
        <ul class="wd-check-list">
          <li v-for="item in overview.bootVolumeRescue || []" :key="item.title">
            <strong>{{ item.title }}</strong>
            <span>{{ item.description }}</span>
          </li>
        </ul>
      </article>

      <article class="wd-card">
        <header><h2><ShieldAlert :size="17" /> netboot.xyz 实验区</h2></header>
        <ul class="wd-check-list">
          <li v-for="item in overview.netbootXyz || []" :key="item.title">
            <strong>{{ item.title }}</strong>
            <span>{{ item.description }}</span>
          </li>
        </ul>
      </article>
    </section>

    <section class="wd-split">
      <article class="wd-card wd-log-card">
        <header>
          <h2><Terminal :size="17" /> 轻量自救检查脚本</h2>
          <button type="button" class="ghost" @click="copy(lightScript)">复制脚本</button>
        </header>
        <pre class="wd-terminal small">{{ lightScript }}</pre>
      </article>

      <article class="wd-card wd-log-card">
        <header>
          <h2><Terminal :size="17" /> netboot.xyz 实验脚本</h2>
          <button type="button" class="ghost" @click="copy(netbootScript)">复制脚本</button>
        </header>
        <pre class="wd-terminal small">{{ netbootScript }}</pre>
      </article>
    </section>

    <article class="wd-card wd-table-card">
      <header><h2>本地救援脚本状态</h2></header>
      <table class="wd-table">
        <thead>
          <tr>
            <th>脚本</th>
            <th>用途</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="script in overview.localScripts || []" :key="script.name">
            <td>{{ script.name }}</td>
            <td>{{ script.description }}</td>
            <td><span class="wd-badge" :class="script.exists ? 'success' : 'warning'">{{ script.exists ? '已同步' : '缺失' }}</span></td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>
