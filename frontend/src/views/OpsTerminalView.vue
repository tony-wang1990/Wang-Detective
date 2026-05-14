<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { FolderOpen, Play, RefreshCw, Save, Server, Square, Terminal, Wifi, Zap } from 'lucide-vue-next';
import { opsGet, opsPost } from '../api/http';

type Host = {
  id?: string;
  name?: string;
  host?: string;
  port?: number;
  username?: string;
  authType?: string;
  tags?: string;
};

type CommandResult = {
  host?: string;
  name?: string;
  exitStatus?: number;
  timedOut?: boolean;
  durationMillis?: number;
  stdout?: string;
  stderr?: string;
};

type SftpEntry = {
  name?: string;
  path?: string;
  directory?: boolean;
  size?: number;
  modifiedTime?: number;
};

const hosts = ref<Host[]>([]);
const selectedHostId = ref('');
const status = ref('');
const output = ref('等待操作...');
const terminalOutput = ref('尚未连接。选择主机后点击 Web SSH，可在这里直接交互。');
const terminalInput = ref('');
const terminalStatus = ref('未连接');
const terminalRef = ref<HTMLElement | null>(null);
const command = ref('uname -a && uptime');
const sftpPath = ref('.');
const sftpEntries = ref<SftpEntry[]>([]);
const form = reactive({
  name: '',
  tags: '',
  host: '',
  port: 22,
  username: 'root',
  authType: 'password',
  password: '',
  privateKey: '',
  passphrase: ''
});
let terminalWs: WebSocket | null = null;

const currentHost = computed(() => hosts.value.find((host) => host.id === selectedHostId.value));

function credential() {
  if (selectedHostId.value) {
    return { hostId: selectedHostId.value };
  }
  return {
    host: form.host,
    port: Number(form.port || 22),
    username: form.username,
    password: form.password,
    privateKey: form.privateKey,
    passphrase: form.passphrase
  };
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败';
}

function fillHost(host: Host) {
  selectedHostId.value = host.id || '';
  form.name = host.name || '';
  form.tags = host.tags || '';
  form.host = host.host || '';
  form.port = Number(host.port || 22);
  form.username = host.username || 'root';
  form.authType = host.authType || 'password';
}

async function loadHosts() {
  try {
    const res = await opsGet<Host[]>('/ssh/hosts');
    hosts.value = res.data || [];
  } catch (error) {
    status.value = errorMessage(error);
  }
}

async function saveHost() {
  status.value = '保存中';
  try {
    const payload = { ...form, port: Number(form.port || 22) };
    await opsPost('/ssh/hosts', payload);
    await loadHosts();
    status.value = '主机已保存';
  } catch (error) {
    status.value = errorMessage(error);
  }
}

async function testConnection() {
  status.value = '测试连接中';
  try {
    const res = await opsPost<boolean>('/ssh/test', { credential: credential() });
    status.value = res.data ? '连接成功' : '连接失败';
  } catch (error) {
    status.value = errorMessage(error);
  }
}

async function execCommand() {
  status.value = '命令执行中';
  try {
    const res = await opsPost<CommandResult>('/ssh/exec', {
      credential: credential(),
      command: command.value,
      timeoutSeconds: 60
    });
    const data = res.data || {};
    output.value = [
      `host: ${data.name || data.host || '-'}`,
      `exit: ${data.exitStatus ?? '-'}`,
      `duration: ${data.durationMillis ?? 0} ms`,
      '',
      data.stdout || '',
      data.stderr ? `\n[stderr]\n${data.stderr}` : ''
    ].join('\n');
    status.value = '命令完成';
  } catch (error) {
    output.value = errorMessage(error);
    status.value = '命令失败';
  }
}

function appendTerminal(text: string) {
  terminalOutput.value += text;
  if (terminalOutput.value.length > 50000) {
    terminalOutput.value = terminalOutput.value.slice(-50000);
  }
  nextTick(() => {
    if (terminalRef.value) {
      terminalRef.value.scrollTop = terminalRef.value.scrollHeight;
    }
  });
}

function disconnectTerminal() {
  if (terminalWs) {
    terminalWs.close();
    terminalWs = null;
  }
  terminalStatus.value = '已断开';
}

async function createSession() {
  disconnectTerminal();
  status.value = '创建 Web SSH 会话中';
  terminalStatus.value = '连接中';
  terminalOutput.value = '正在创建 SSH 会话...\n';
  try {
    const res = await opsPost<{ websocketPath?: string; sessionId?: string }>('/ssh/session', {
      credential: credential(),
      ttlMinutes: 30
    });
    const websocketPath = res.data?.websocketPath;
    if (!websocketPath) {
      terminalStatus.value = '创建失败';
      status.value = '会话创建失败';
      appendTerminal('后端没有返回 WebSocket 地址。\n');
      return;
    }
    const token = sessionStorage.getItem('token') || '';
    const scheme = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const url = `${scheme}//${window.location.host}${websocketPath}?token=${encodeURIComponent(token)}`;
    terminalWs = new WebSocket(url);
    terminalWs.onopen = () => {
      terminalStatus.value = '已连接';
      status.value = 'Web SSH 已连接';
    };
    terminalWs.onmessage = (event) => appendTerminal(String(event.data || ''));
    terminalWs.onerror = () => {
      terminalStatus.value = '连接错误';
      status.value = 'Web SSH 连接错误';
    };
    terminalWs.onclose = () => {
      terminalWs = null;
      terminalStatus.value = '已断开';
    };
  } catch (error) {
    terminalStatus.value = '连接失败';
    status.value = 'Web SSH 连接失败';
    appendTerminal(`[本地] ${errorMessage(error)}\n`);
  }
}

function sendTerminalLine() {
  if (!terminalWs || terminalWs.readyState !== WebSocket.OPEN) {
    appendTerminal('\n[本地] 终端未连接，请先点击 Web SSH。\n');
    return;
  }
  terminalWs.send(`${terminalInput.value}\n`);
  terminalInput.value = '';
}

function sendCtrlC() {
  if (terminalWs && terminalWs.readyState === WebSocket.OPEN) {
    terminalWs.send('\u0003');
  }
}

async function listSftp(path = sftpPath.value) {
  status.value = '读取 SFTP 目录中';
  try {
    const res = await opsPost<{ path?: string; entries?: SftpEntry[] }>('/sftp/list', {
      credential: credential(),
      path
    });
    sftpPath.value = res.data?.path || path;
    sftpEntries.value = res.data?.entries || [];
    status.value = 'SFTP 已刷新';
  } catch (error) {
    status.value = errorMessage(error);
  }
}

onMounted(loadHosts);
onBeforeUnmount(disconnectTerminal);
</script>

<template>
  <section class="wd-page">
    <div class="wd-page-title">
      <div>
        <h1>运维终端</h1>
        <p>已迁入 Vue 原生路由，保留主机库、SSH 命令、Web SSH 会话和 SFTP 浏览入口。</p>
      </div>
      <div class="wd-actions">
        <button type="button" @click="loadHosts"><RefreshCw :size="16" />刷新主机</button>
        <button type="button" @click="testConnection"><Wifi :size="16" />测试连接</button>
      </div>
    </div>

    <section class="wd-ops-grid">
      <aside class="wd-card wd-form-card">
        <header><h2><Server :size="17" /> 连接</h2></header>
        <div class="wd-form-grid single">
          <label>
            <span>保存的主机</span>
            <select v-model="selectedHostId" @change="currentHost && fillHost(currentHost)">
              <option value="">手动输入/临时主机</option>
              <option v-for="host in hosts" :key="host.id" :value="host.id">{{ host.name || host.host }}</option>
            </select>
          </label>
          <label><span>主机名称</span><input v-model="form.name" placeholder="例如 tokyo-a1" /></label>
          <label><span>标签</span><input v-model="form.tags" placeholder="oci,prod,tokyo" /></label>
          <div class="wd-two">
            <label><span>主机</span><input v-model="form.host" placeholder="1.2.3.4" /></label>
            <label><span>端口</span><input v-model.number="form.port" type="number" /></label>
          </div>
          <label><span>用户</span><input v-model="form.username" /></label>
          <label>
            <span>认证</span>
            <select v-model="form.authType">
              <option value="password">密码</option>
              <option value="privateKey">私钥</option>
            </select>
          </label>
          <label v-if="form.authType === 'password'"><span>密码</span><input v-model="form.password" type="password" /></label>
          <label v-else><span>私钥</span><textarea v-model="form.privateKey" /></label>
          <div class="wd-actions compact">
            <button type="button" @click="saveHost"><Save :size="16" />保存</button>
            <button type="button" @click="createSession"><Terminal :size="16" />Web SSH</button>
          </div>
        </div>
      </aside>

      <main class="wd-ops-workspace">
        <div class="wd-card wd-log-card">
          <header>
            <h2><Terminal :size="17" /> SSH 命令</h2>
            <span>{{ status || '待命' }}</span>
          </header>
          <div class="wd-command-row">
            <input v-model="command" placeholder="输入命令" @keyup.enter="execCommand" />
            <button type="button" @click="execCommand"><Play :size="16" />执行</button>
          </div>
          <pre class="wd-terminal">{{ output }}</pre>
        </div>

        <div class="wd-card wd-log-card">
          <header>
            <h2><Terminal :size="17" /> Web SSH</h2>
            <span>{{ terminalStatus }}</span>
          </header>
          <pre ref="terminalRef" class="wd-terminal">{{ terminalOutput }}</pre>
          <div class="wd-command-row terminal-input-row">
            <input v-model="terminalInput" placeholder="输入命令，Enter 发送" @keyup.enter="sendTerminalLine" />
            <button type="button" @click="sendTerminalLine"><Zap :size="16" />发送</button>
            <button type="button" @click="sendCtrlC"><Square :size="14" />Ctrl+C</button>
            <button type="button" class="ghost" @click="disconnectTerminal">断开</button>
          </div>
        </div>

        <div class="wd-card">
          <header>
            <h2><FolderOpen :size="17" /> SFTP 浏览</h2>
            <button type="button" @click="listSftp()"><RefreshCw :size="16" />刷新</button>
          </header>
          <div class="wd-command-row">
            <input v-model="sftpPath" placeholder="远程目录，例如 /root" @keyup.enter="listSftp()" />
            <button type="button" @click="listSftp()">打开</button>
          </div>
          <table class="wd-table">
            <thead><tr><th>名称</th><th>类型</th><th>大小</th><th>路径</th></tr></thead>
            <tbody>
              <tr v-if="sftpEntries.length === 0"><td colspan="4">暂无目录数据</td></tr>
              <tr v-for="entry in sftpEntries" :key="entry.path || entry.name" @dblclick="entry.directory && listSftp(entry.path)">
                <td>{{ entry.name }}</td>
                <td>{{ entry.directory ? '目录' : '文件' }}</td>
                <td>{{ entry.size || 0 }}</td>
                <td>{{ entry.path }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </main>
    </section>
  </section>
</template>
