<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { LifeBuoy, Play, RefreshCw, Server, ShieldAlert, Terminal } from 'lucide-vue-next';
import { apiGet, apiPost, notifyGlobal, type PageResult } from '../api/http';

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

type OciConfig = {
  id?: string;
  username?: string;
  tenantName?: string;
  region?: string;
};

type InstanceInfo = {
  ocId?: string;
  name?: string;
  publicIp?: string[];
  state?: string;
  shape?: string;
};

type OciDetail = {
  instanceList?: InstanceInfo[];
};

const loading = ref(false);
const actionLoading = ref('');
const error = ref('');
const notice = ref('');
const overview = ref<RescueOverview>({});
const lightScript = ref('');
const netbootScript = ref('');
const configs = ref<OciConfig[]>([]);
const instances = ref<InstanceInfo[]>([]);
const selectedConfigId = ref('');
const selectedInstanceId = ref('');
const rescueName = ref('');
const keepBackupVolume = ref(true);

const selectedConfig = computed(() => configs.value.find((item) => item.id === selectedConfigId.value));
const selectedInstance = computed(() => instances.value.find((item) => item.ocId === selectedInstanceId.value));

async function loadOverview() {
  loading.value = true;
  error.value = '';
  try {
    const [overviewRsp, lightRsp, netbootRsp] = await Promise.all([
      apiGet<RescueOverview>('/rescue/overview'),
      apiGet<string>('/rescue/light-script'),
      apiGet<string>('/rescue/netboot-script?mode=ipxe')
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

async function loadConfigs() {
  actionLoading.value = 'configs';
  error.value = '';
  try {
    const res = await apiPost<PageResult<OciConfig>>('/oci/userPage', {
      keyword: '',
      currentPage: 1,
      pageSize: 200,
      isEnableCreate: null
    });
    configs.value = res.data?.records || [];
    if (!selectedConfigId.value && configs.value[0]?.id) {
      selectedConfigId.value = configs.value[0].id;
      await loadInstances();
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '读取 OCI 配置失败';
  } finally {
    actionLoading.value = '';
  }
}

async function loadInstances() {
  if (!selectedConfigId.value) {
    instances.value = [];
    selectedInstanceId.value = '';
    return;
  }
  actionLoading.value = 'instances';
  error.value = '';
  try {
    const res = await apiPost<OciDetail>('/oci/details', {
      cfgId: selectedConfigId.value,
      cleanReLaunchDetails: false
    });
    instances.value = res.data?.instanceList || [];
    if (!selectedInstanceId.value || !instances.value.some((item) => item.ocId === selectedInstanceId.value)) {
      selectedInstanceId.value = instances.value[0]?.ocId || '';
    }
    if (selectedInstance.value && !rescueName.value) {
      rescueName.value = `${selectedInstance.value.name || 'oci-instance'}-rescue`;
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '读取实例列表失败';
  } finally {
    actionLoading.value = '';
  }
}

async function submitAutoRescue() {
  if (!selectedConfigId.value || !selectedInstanceId.value) {
    error.value = '请先选择 OCI 配置和失联实例';
    return;
  }
  actionLoading.value = 'autoRescue';
  error.value = '';
  try {
    await apiPost('/oci/autoRescue', {
      ociCfgId: selectedConfigId.value,
      instanceId: selectedInstanceId.value,
      name: rescueName.value || `${selectedInstance.value?.name || 'instance'}-rescue`,
      keepBackupVolume: keepBackupVolume.value
    });
    notice.value = '自动救援任务已下发，请在任务列表、操作审计和 OCI 控制台查看进度。';
    notifyGlobal(notice.value, 'success');
  } catch (err) {
    error.value = err instanceof Error ? err.message : '提交自动救援失败';
    notifyGlobal(error.value, 'error');
  } finally {
    actionLoading.value = '';
  }
}

async function copy(text: string) {
  if (!text) return;
  await navigator.clipboard.writeText(text);
  notifyGlobal('脚本已复制', 'success');
}

onMounted(() => {
  loadOverview();
  loadConfigs();
});
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
    <p v-if="notice" class="wd-notice">{{ notice }}</p>
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

    <section class="wd-split">
      <article class="wd-card wd-form-card">
        <header><h2><LifeBuoy :size="17" /> 一键自动救援</h2></header>
        <p class="wd-help-line">真实调用 OCI 自动救援接口，适合实例失联、SSH 异常或需要拆卷修复的场景；高危动作会保留备份卷选项。</p>
        <div class="wd-form-grid single">
          <label>
            <span>OCI 配置</span>
            <select v-model="selectedConfigId" :disabled="Boolean(actionLoading)" @change="selectedInstanceId = ''; rescueName = ''; loadInstances()">
              <option value="">请选择 OCI 配置</option>
              <option v-for="cfg in configs" :key="cfg.id" :value="cfg.id">
                {{ cfg.username || cfg.tenantName || cfg.id }} / {{ cfg.region || '-' }}
              </option>
            </select>
          </label>
          <label>
            <span>失联或待救援实例</span>
            <select v-model="selectedInstanceId" :disabled="Boolean(actionLoading) || !instances.length" @change="rescueName = `${selectedInstance?.name || 'oci-instance'}-rescue`">
              <option value="">{{ instances.length ? '请选择实例' : '当前配置暂无实例' }}</option>
              <option v-for="item in instances" :key="item.ocId" :value="item.ocId">
                {{ item.name || item.ocId }} / {{ item.state || '-' }} / {{ item.shape || '-' }}
              </option>
            </select>
          </label>
          <label>
            <span>救援实例名称</span>
            <input v-model="rescueName" placeholder="例如 instance-a-rescue" />
          </label>
          <label class="wd-switch-row">
            <input v-model="keepBackupVolume" type="checkbox" />
            <span>保留备份卷，便于回滚</span>
          </label>
          <div class="wd-actions compact">
            <button type="button" class="ghost" :disabled="!selectedConfigId || Boolean(actionLoading)" @click="loadInstances">
              <RefreshCw :size="16" />刷新实例
            </button>
            <button type="button" class="danger" :disabled="!selectedConfigId || !selectedInstanceId || actionLoading === 'autoRescue'" @click="submitAutoRescue">
              <Play :size="16" />{{ actionLoading === 'autoRescue' ? '下发中...' : '一键自动救援' }}
            </button>
          </div>
        </div>
      </article>

      <article class="wd-card wd-detail-card">
        <header><h2><Server :size="17" /> 当前目标</h2></header>
        <div class="wd-detail-summary">
          <div><span>配置</span><strong>{{ selectedConfig?.username || selectedConfig?.tenantName || '-' }}</strong></div>
          <div><span>区域</span><strong>{{ selectedConfig?.region || '-' }}</strong></div>
          <div><span>实例</span><strong>{{ selectedInstance?.name || selectedInstance?.ocId || '-' }}</strong></div>
          <div><span>公网 IP</span><strong>{{ selectedInstance?.publicIp?.join(', ') || '-' }}</strong></div>
        </div>
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
