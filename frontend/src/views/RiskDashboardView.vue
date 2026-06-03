<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { AlertTriangle, HardDrive, RefreshCw, Server, ShieldAlert, ShieldCheck, Wifi } from 'lucide-vue-next';
import { apiGet } from '../api/http';

type RiskSummary = {
  configCount?: number;
  scannedConfigCount?: number;
  instanceCount?: number;
  runningInstanceCount?: number;
  stoppedInstanceCount?: number;
  armInstanceCount?: number;
  armOcpus?: number;
  armMemoryGb?: number;
  bootVolumeGb?: number;
  highRiskCount?: number;
  warnRiskCount?: number;
  errorConfigCount?: number;
};

type ConfigRisk = {
  configId?: string;
  configName?: string;
  region?: string;
  instanceCount?: number;
  runningInstanceCount?: number;
  armOcpus?: number;
  armMemoryGb?: number;
  bootVolumeGb?: number;
  publicIngressRuleCount?: number;
  highRiskPortRuleCount?: number;
  resourceRisks?: ResourceRisk[];
  portExposures?: PortExposure[];
  status?: string;
  message?: string;
};

type ResourceRisk = {
  level?: string;
  category?: string;
  title?: string;
  message?: string;
  recommendation?: string;
  count?: number;
  configName?: string;
  region?: string;
};

type PortExposure = {
  configName?: string;
  region?: string;
  vcnName?: string;
  source?: string;
  protocol?: string;
  portRange?: string;
  description?: string;
  highRisk?: boolean;
  recommendation?: string;
};

type RiskItem = {
  level?: string;
  category?: string;
  title?: string;
  message?: string;
  configName?: string;
  region?: string;
  portExposures?: PortExposure[];
};

type RiskReport = {
  generatedAt?: string;
  summary?: RiskSummary;
  configs?: ConfigRisk[];
  risks?: RiskItem[];
};

const loading = ref(false);
const error = ref('');
const report = ref<RiskReport | null>(null);
const maxConfigs = ref(8);

const summary = computed<RiskSummary>(() => report.value?.summary || {});
const configs = computed(() => report.value?.configs || []);
const risks = computed(() => report.value?.risks || []);
const highRisks = computed(() => risks.value.filter((item) => item.level === 'HIGH'));
const warnRisks = computed(() => risks.value.filter((item) => item.level === 'WARN'));
const errorRisks = computed(() => risks.value.filter((item) => item.level === 'ERROR'));
const resourceRisks = computed<ResourceRisk[]>(() =>
  configs.value.flatMap((cfg) =>
    (cfg.resourceRisks || []).map((item) => ({
      ...item,
      configName: cfg.configName || cfg.configId,
      region: cfg.region
    }))
  )
);
const highResourceRisks = computed(() => resourceRisks.value.filter((item) => item.level === 'HIGH' || item.level === 'ERROR'));
const portExposures = computed<PortExposure[]>(() =>
  configs.value.flatMap((cfg) =>
    (cfg.portExposures || []).map((item) => ({
      ...item,
      configName: cfg.configName || cfg.configId,
      region: cfg.region
    }))
  )
);

function levelClass(level?: string) {
  if (level === 'HIGH' || level === 'ERROR') return 'danger';
  if (level === 'WARN') return 'warning';
  return 'success';
}

function statusText(status?: string) {
  if (status === 'HIGH') return '高风险';
  if (status === 'WARN') return '注意';
  if (status === 'ERROR') return '异常';
  return '正常';
}

function formatTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

function exposureLabel(item: PortExposure) {
  return item.highRisk ? '高危' : '公开';
}

async function loadReport() {
  loading.value = true;
  error.value = '';
  try {
    const res = await apiGet<RiskReport>(`/v1/oci/risk?maxConfigs=${maxConfigs.value}`);
    report.value = res.data || null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '风险看板读取失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadReport);
</script>

<template>
  <section class="wd-page">
    <div class="wd-page-title">
      <div>
        <h1>风险看板</h1>
        <p>实时扫描 OCI 配置、实例状态、ARM 免费资源、引导卷容量和安全列表风险。</p>
      </div>
      <div class="wd-actions">
        <select v-model.number="maxConfigs" class="wd-compact-select">
          <option :value="5">扫描 5 个配置</option>
          <option :value="8">扫描 8 个配置</option>
          <option :value="20">扫描 20 个配置</option>
          <option :value="50">扫描 50 个配置</option>
        </select>
        <button type="button" @click="loadReport" :disabled="loading">
          <RefreshCw :size="16" />{{ loading ? '扫描中...' : '重新扫描' }}
        </button>
      </div>
    </div>

    <p v-if="error" class="wd-error-line">{{ error }}</p>

    <section class="wd-log-summary">
      <article class="wd-card wd-stat-card">
        <span>扫描配置</span>
        <strong><ShieldCheck :size="20" />{{ summary.scannedConfigCount || 0 }} / {{ summary.configCount || 0 }}</strong>
      </article>
      <article class="wd-card wd-stat-card">
        <span>实例运行</span>
        <strong><Server :size="20" />{{ summary.runningInstanceCount || 0 }} / {{ summary.instanceCount || 0 }}</strong>
      </article>
      <article class="wd-card wd-stat-card">
        <span>ARM 用量</span>
        <strong>{{ summary.armOcpus || 0 }} OCPU / {{ summary.armMemoryGb || 0 }} GB</strong>
      </article>
      <article class="wd-card wd-stat-card">
            <span>资源风险</span>
            <strong><ShieldAlert :size="20" />{{ highResourceRisks.length }} 高 / {{ resourceRisks.filter((item) => item.level === 'WARN').length }} 警告</strong>
      </article>
    </section>

    <section class="wd-risk-grid">
      <article class="wd-card">
        <header>
          <h2><AlertTriangle :size="17" /> 风险清单</h2>
          <span class="wd-help-line">生成时间 {{ formatTime(report?.generatedAt) }}</span>
        </header>
        <div class="wd-risk-list">
          <div v-for="item in risks" :key="`${item.level}-${item.title}-${item.configName}`" class="wd-risk-item">
            <span class="wd-badge" :class="levelClass(item.level)">{{ item.level }}</span>
            <div>
              <b>{{ item.title }}</b>
              <p>{{ item.message }}</p>
              <small>{{ item.configName || '-' }} / {{ item.region || '-' }} / {{ item.category || '-' }}</small>
              <ul v-if="item.portExposures?.length" class="wd-risk-mini-list">
                <li v-for="exposure in item.portExposures.slice(0, 3)" :key="`${exposure.vcnName}-${exposure.source}-${exposure.portRange}`">
                  {{ exposure.protocol }} {{ exposure.portRange }} · {{ exposure.source }} · {{ exposure.recommendation }}
                </li>
              </ul>
            </div>
          </div>
          <div v-if="!risks.length" class="wd-risk-empty">
            <ShieldCheck :size="28" />
            当前扫描范围没有发现高优先级风险。
          </div>
        </div>
      </article>

      <article class="wd-card">
        <header><h2><HardDrive :size="17" /> 资源概览</h2></header>
        <div class="wd-health-list">
          <div>
            <ShieldAlert :size="18" />
            <span>资源/配置高危</span>
            <em class="error">{{ highResourceRisks.length }}</em>
          </div>
          <div>
            <AlertTriangle :size="18" />
            <span>警告</span>
            <em class="warn">{{ warnRisks.length }}</em>
          </div>
          <div>
            <Wifi :size="18" />
            <span>扫描异常</span>
            <em class="error">{{ errorRisks.length }}</em>
          </div>
          <div>
            <HardDrive :size="18" />
            <span>引导卷容量</span>
            <em>{{ summary.bootVolumeGb || 0 }} GB</em>
          </div>
        </div>
      </article>
    </section>

    <article class="wd-card wd-table-card">
      <header>
        <h2><ShieldAlert :size="17" /> 配置资源风险</h2>
        <span class="wd-help-line">优先看这里：实例状态、资源用量、引导卷、配置异常和网络开放都会汇总为可处理事项。</span>
      </header>
      <div class="wd-table-scroll">
        <table class="wd-table">
          <thead>
            <tr>
              <th>配置</th>
              <th>类别</th>
              <th>级别</th>
              <th>问题</th>
              <th>建议</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in resourceRisks" :key="`${item.configName}-${item.category}-${item.title}`">
              <td><b>{{ item.configName || '-' }}</b><small>{{ item.region || '-' }}</small></td>
              <td>{{ item.category || '-' }}</td>
              <td><span class="wd-badge" :class="levelClass(item.level)">{{ item.level }}</span></td>
              <td><b>{{ item.title }}</b><small>{{ item.message || '-' }}</small></td>
              <td class="wd-port-suggestion">{{ item.recommendation || '确认是否符合当前业务预期。' }}</td>
            </tr>
            <tr v-if="!resourceRisks.length">
              <td colspan="5" class="wd-empty">暂无配置/资源风险。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <article class="wd-card wd-table-card">
      <header>
        <h2><ShieldAlert :size="17" /> 公网规则明细（网络项）</h2>
        <span class="wd-help-line">来自 OCI 安全列表入站规则，网络开放证据在这里展开；需要修改请到配置列表的规则明细。</span>
      </header>
      <div class="wd-table-scroll">
        <table class="wd-table">
          <thead>
            <tr>
              <th>配置</th>
              <th>VCN</th>
              <th>来源</th>
              <th>协议/端口</th>
              <th>风险</th>
              <th>收敛建议</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in portExposures" :key="`${item.configName}-${item.vcnName}-${item.source}-${item.protocol}-${item.portRange}`">
              <td><b>{{ item.configName || '-' }}</b><small>{{ item.region || '-' }}</small></td>
              <td>{{ item.vcnName || '-' }}</td>
              <td>{{ item.source || '-' }}</td>
              <td><b>{{ item.protocol || '-' }}</b><small>{{ item.portRange || '-' }} · {{ item.description || '-' }}</small></td>
              <td><span class="wd-badge" :class="item.highRisk ? 'danger' : 'warning'">{{ exposureLabel(item) }}</span></td>
              <td class="wd-port-suggestion">{{ item.recommendation || '确认业务必要性，尽量收敛公网来源。' }}</td>
            </tr>
            <tr v-if="!portExposures.length">
              <td colspan="6" class="wd-empty">暂无公网入站端口暴露。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <article class="wd-card wd-table-card">
      <header><h2><ShieldAlert :size="17" /> 配置风险明细</h2></header>
      <div class="wd-table-scroll">
        <table class="wd-table">
          <thead>
            <tr>
              <th>配置</th>
              <th>区域</th>
              <th>实例</th>
              <th>ARM 用量</th>
              <th>引导卷</th>
              <th>资源风险</th>
              <th>公网入站</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="cfg in configs" :key="cfg.configId || cfg.configName">
              <td><b>{{ cfg.configName || cfg.configId }}</b><small>{{ cfg.message }}</small></td>
              <td>{{ cfg.region || '-' }}</td>
              <td>{{ cfg.runningInstanceCount || 0 }} / {{ cfg.instanceCount || 0 }}</td>
              <td>{{ cfg.armOcpus || 0 }} OCPU / {{ cfg.armMemoryGb || 0 }} GB</td>
              <td>{{ cfg.bootVolumeGb || 0 }} GB</td>
              <td>{{ cfg.resourceRisks?.length || 0 }} 项</td>
              <td>
                {{ cfg.publicIngressRuleCount || 0 }} 条，其中高危 {{ cfg.highRiskPortRuleCount || 0 }} 条
                <small>端口明细 {{ cfg.portExposures?.length || 0 }} 条</small>
              </td>
              <td><span class="wd-badge" :class="levelClass(cfg.status)">{{ statusText(cfg.status) }}</span></td>
            </tr>
            <tr v-if="!configs.length">
              <td colspan="8" class="wd-empty">暂无扫描结果。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>
  </section>
</template>
