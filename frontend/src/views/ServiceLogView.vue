<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { FileText, Pause, Play, Trash2 } from 'lucide-vue-next';

const lines = ref<string[]>([]);
const status = ref('未连接');
let ws: WebSocket | null = null;

function connect() {
  const token = sessionStorage.getItem('token') || '';
  if (!token) {
    status.value = '缺少登录 token';
    return;
  }
  close();
  const url = `${window.location.origin.replace(/^http/, 'ws')}/logs?token=${encodeURIComponent(token)}`;
  ws = new WebSocket(url);
  status.value = '连接中';
  ws.onopen = () => {
    status.value = '实时推送中';
  };
  ws.onmessage = (event) => {
    lines.value.push(String(event.data));
    if (lines.value.length > 500) lines.value.shift();
  };
  ws.onerror = () => {
    status.value = '连接异常';
  };
  ws.onclose = () => {
    status.value = '已断开';
  };
}

function close() {
  if (ws) {
    ws.close();
    ws = null;
  }
}

onMounted(connect);
onBeforeUnmount(close);
</script>

<template>
  <section class="wd-page">
    <div class="wd-page-title">
      <div>
        <h1>服务日志</h1>
        <p>通过 WebSocket 实时查看后端日志，保留最近 500 行。</p>
      </div>
      <div class="wd-actions">
        <button type="button" @click="connect"><Play :size="16" />连接</button>
        <button type="button" @click="close"><Pause :size="16" />断开</button>
        <button type="button" class="danger" @click="lines = []"><Trash2 :size="16" />清空</button>
      </div>
    </div>

    <div class="wd-card wd-log-card">
      <header>
        <h2><FileText :size="17" /> 实时日志</h2>
        <span>{{ status }}</span>
      </header>
      <pre class="wd-terminal">{{ lines.length ? lines.join('\n') : '等待日志推送...' }}</pre>
    </div>
  </section>
</template>
