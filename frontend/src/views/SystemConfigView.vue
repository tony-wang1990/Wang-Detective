<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { Bell, Database, RefreshCw, Save, ShieldCheck } from 'lucide-vue-next';
import { apiGet, apiPost } from '../api/http';

type DiagItem = {
  name?: string;
  key?: string;
  status?: string;
  message?: string;
  detail?: string;
};

const loading = ref(false);
const cfg = reactive<Record<string, string | boolean>>({});
const diagnostics = ref<DiagItem[]>([]);
const raw = ref('');
const msg = ref('');
const mfaCode = ref('');
const notice = ref('');

async function loadCfg() {
  loading.value = true;
  try {
    const res = await apiPost<Record<string, string | boolean>>('/sys/getSysCfg', {});
    Object.assign(cfg, res.data || {});
  } catch (err) {
    notice.value = err instanceof Error ? err.message : '读取配置失败';
  } finally {
    loading.value = false;
  }
}

async function saveCfg() {
  try {
    const res = await apiPost<void>('/sys/updateSysCfg', cfg);
    notice.value = res.msg || '保存成功';
  } catch (err) {
    notice.value = err instanceof Error ? err.message : '保存失败';
  }
}

async function loadDiagnostics() {
  try {
    const res = await apiGet<Record<string, unknown>>('/v1/system/diagnostics');
    raw.value = JSON.stringify(res.data || res, null, 2);
    const checks = (res.data?.checks || []) as DiagItem[];
    diagnostics.value = checks;
  } catch (err) {
    raw.value = err instanceof Error ? err.message : '诊断读取失败';
  }
}

async function sendTestMessage() {
  const res = await apiPost<void>('/sys/sendMsg', { message: msg.value || 'W-探长测试消息' });
  notice.value = res.msg || '测试消息已发送';
}

async function checkMfa() {
  const res = await apiPost<void>('/sys/checkMfaCode', { mfaCode: mfaCode.value });
  notice.value = res.msg || 'MFA 验证通过';
}

onMounted(() => {
  loadCfg();
  loadDiagnostics();
});
</script>

<template>
  <section class="wd-page">
    <div class="wd-page-title">
      <div>
        <h1>系统配置</h1>
        <p>Telegram、通知、AI、MFA 和系统诊断统一入口，已跟随新版深浅色框架。</p>
      </div>
      <div class="wd-actions">
        <button type="button" @click="loadDiagnostics"><RefreshCw :size="16" />刷新诊断</button>
        <button type="button" @click="saveCfg"><Save :size="16" />保存配置</button>
      </div>
    </div>

    <p v-if="notice" class="wd-notice">{{ notice }}</p>

    <section class="wd-split">
      <div class="wd-card wd-form-card">
        <header><h2><Bell :size="17" /> 基础配置</h2></header>
        <div class="wd-form-grid">
          <label v-for="(_, key) in cfg" :key="key">
            <span>{{ key }}</span>
            <input v-model="cfg[key]" :placeholder="String(key)" />
          </label>
          <p v-if="!Object.keys(cfg).length && !loading" class="wd-muted-line">暂无可编辑配置或接口未返回配置。</p>
        </div>
      </div>

      <div class="wd-card wd-form-card">
        <header><h2><ShieldCheck :size="17" /> 测试工具</h2></header>
        <div class="wd-form-grid single">
          <label>
            <span>测试消息</span>
            <textarea v-model="msg" placeholder="输入要发送到 Telegram/钉钉的测试消息" />
          </label>
          <button type="button" @click="sendTestMessage">发送测试消息</button>
          <label>
            <span>MFA 验证码</span>
            <input v-model="mfaCode" placeholder="输入 MFA 验证码" />
          </label>
          <button type="button" @click="checkMfa">验证 MFA</button>
        </div>
      </div>
    </section>

    <section class="wd-split">
      <div class="wd-card">
        <header><h2><Database :size="17" /> 系统诊断</h2></header>
        <div class="wd-health-list">
          <div v-for="item in diagnostics" :key="item.key || item.name">
            <Database :size="18" />
            <b>{{ item.name || item.key }}</b>
            <em :class="String(item.status || '').toLowerCase()">{{ item.status || 'INFO' }}</em>
            <small>{{ item.message || item.detail }}</small>
          </div>
        </div>
      </div>
      <div class="wd-card wd-log-card">
        <header><h2>接口原始返回</h2></header>
        <pre class="wd-terminal small">{{ raw || '暂无诊断返回' }}</pre>
      </div>
    </section>
  </section>
</template>
