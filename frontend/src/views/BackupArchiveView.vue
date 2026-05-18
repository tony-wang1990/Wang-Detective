<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Archive, CloudUpload, Clock, DatabaseBackup, RefreshCw, RotateCcw, Trash2 } from 'lucide-vue-next';
import { apiGet, apiPost, notifyGlobal, type PageResult } from '../api/http';

type OciConfig = {
  id?: string;
  username?: string;
  tenantName?: string;
  region?: string;
};

type BucketInfo = {
  name?: string;
  namespaceName?: string;
  storageTier?: string;
  timeCreated?: string;
};

type ObjectInfo = {
  name?: string;
  sizeBytes?: number;
  md5?: string;
  timeCreated?: string;
};

type ObjectList = {
  namespaceName?: string;
  bucketName?: string;
  prefix?: string;
  objects?: ObjectInfo[];
};

type BackupResult = {
  localPath?: string;
  namespaceName?: string;
  bucketName?: string;
  objectName?: string;
  sizeBytes?: number;
  md5?: string;
  createTime?: string;
};

type LocalBackupInfo = {
  name?: string;
  path?: string;
  sizeBytes?: number;
  modifiedTime?: string;
};

type RestorePlan = {
  backupName?: string;
  backupPath?: string;
  sizeBytes?: number;
  command?: string;
  steps?: string[];
  warnings?: string[];
};

type SchedulePlan = {
  cronExpression?: string;
  command?: string;
  steps?: string[];
  objectStoragePolicy?: string[];
};

type DeleteConfirm = {
  objectName: string;
};

const configs = ref<OciConfig[]>([]);
const buckets = ref<BucketInfo[]>([]);
const objects = ref<ObjectInfo[]>([]);
const selectedConfigId = ref('');
const selectedBucket = ref('');
const prefix = ref('wang-detective/backups');
const includeLogs = ref(false);
const uploadToObjectStorage = ref(true);
const loading = ref('');
const error = ref('');
const notice = ref('');
const lastBackup = ref<BackupResult | null>(null);
const localBackups = ref<LocalBackupInfo[]>([]);
const selectedBackupName = ref('');
const restorePlan = ref<RestorePlan | null>(null);
const cronExpression = ref('0 3 * * *');
const schedulePlan = ref<SchedulePlan | null>(null);
const deleteConfirm = ref<DeleteConfirm | null>(null);

const selectedConfig = computed(() => configs.value.find((item) => item.id === selectedConfigId.value));
const objectCount = computed(() => objects.value.length);
const totalSize = computed(() => objects.value.reduce((sum, item) => sum + Number(item.sizeBytes || 0), 0));

function formatBytes(bytes?: number) {
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = Number(bytes || 0);
  let index = 0;
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024;
    index += 1;
  }
  return `${value.toFixed(index ? 1 : 0)} ${units[index]}`;
}

function formatTime(value?: string) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 19);
}

async function loadConfigs() {
  loading.value = 'configs';
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
      await loadBuckets();
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '读取 OCI 配置失败';
  } finally {
    loading.value = '';
  }
}

async function loadBuckets() {
  if (!selectedConfigId.value) {
    return;
  }
  loading.value = 'buckets';
  error.value = '';
  try {
    const res = await apiGet<BucketInfo[]>(`/v1/backups/buckets?ociCfgId=${encodeURIComponent(selectedConfigId.value)}`);
    buckets.value = res.data || [];
    if (!selectedBucket.value && buckets.value[0]?.name) {
      selectedBucket.value = buckets.value[0].name;
    }
    await loadObjects();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '读取对象存储 Bucket 失败';
  } finally {
    loading.value = '';
  }
}

async function loadObjects() {
  if (!selectedConfigId.value || !selectedBucket.value) {
    objects.value = [];
    return;
  }
  loading.value = 'objects';
  error.value = '';
  try {
    const res = await apiPost<ObjectList>('/v1/backups/objects', {
      ociCfgId: selectedConfigId.value,
      bucketName: selectedBucket.value,
      prefix: prefix.value,
      limit: 200
    });
    objects.value = res.data?.objects || [];
    notice.value = `已刷新归档列表：${new Date().toLocaleTimeString()}`;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '读取归档对象失败';
  } finally {
    loading.value = '';
  }
}

async function createArchive() {
  if (uploadToObjectStorage.value && !selectedBucket.value) {
    error.value = '上传对象存储时必须选择 Bucket';
    return;
  }
  loading.value = 'archive';
  error.value = '';
  try {
    const res = await apiPost<BackupResult>('/v1/backups/archive', {
      ociCfgId: selectedConfigId.value,
      bucketName: selectedBucket.value,
      prefix: prefix.value,
      includeLogs: includeLogs.value,
      uploadToObjectStorage: uploadToObjectStorage.value
    });
    lastBackup.value = res.data || null;
    notice.value = uploadToObjectStorage.value ? '备份已生成并上传对象存储' : '本地备份包已生成';
    notifyGlobal(notice.value, 'success');
    await loadLocalBackups();
    await loadObjects();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '创建备份失败';
    notifyGlobal(error.value, 'error');
  } finally {
    loading.value = '';
  }
}

async function loadLocalBackups() {
  loading.value = 'localBackups';
  error.value = '';
  try {
    const res = await apiGet<LocalBackupInfo[]>('/v1/backups/local?limit=20');
    localBackups.value = res.data || [];
    if (!selectedBackupName.value && localBackups.value[0]?.name) {
      selectedBackupName.value = localBackups.value[0].name;
      await loadRestorePlan();
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '读取本地备份失败';
  } finally {
    loading.value = '';
  }
}

async function loadRestorePlan() {
  if (!selectedBackupName.value) {
    restorePlan.value = null;
    return;
  }
  loading.value = 'restorePlan';
  error.value = '';
  try {
    const res = await apiGet<RestorePlan>(`/v1/backups/restore-plan?backupName=${encodeURIComponent(selectedBackupName.value)}`);
    restorePlan.value = res.data || null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '生成恢复方案失败';
  } finally {
    loading.value = '';
  }
}

async function loadSchedulePlan() {
  loading.value = 'schedulePlan';
  error.value = '';
  try {
    const res = await apiGet<SchedulePlan>(`/v1/backups/schedule-plan?cron=${encodeURIComponent(cronExpression.value)}`);
    schedulePlan.value = res.data || null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '生成定时备份方案失败';
  } finally {
    loading.value = '';
  }
}

async function copyText(text?: string) {
  if (!text) return;
  await navigator.clipboard.writeText(text);
  notifyGlobal('命令已复制', 'success');
}

function requestDeleteObject(item: ObjectInfo) {
  if (!item.name) {
    return;
  }
  deleteConfirm.value = { objectName: item.name };
}

async function deleteObject() {
  const current = deleteConfirm.value;
  if (!current?.objectName) return;
  loading.value = `delete:${current.objectName}`;
  error.value = '';
  try {
    await apiPost('/v1/backups/delete-object', {
      ociCfgId: selectedConfigId.value,
      bucketName: selectedBucket.value,
      objectName: current.objectName
    });
    notice.value = '对象已删除';
    notifyGlobal(notice.value, 'success');
    deleteConfirm.value = null;
    await loadObjects();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除对象失败';
    notifyGlobal(error.value, 'error');
  } finally {
    loading.value = '';
  }
}

onMounted(() => {
  loadConfigs();
  loadLocalBackups();
  loadSchedulePlan();
});
</script>

<template>
  <section class="wd-page">
    <div class="wd-page-title">
      <div>
        <h1>备份归档</h1>
        <p>把 data、keys、配置文件和脚本打成备份包，并可同步归档到 OCI Object Storage。</p>
      </div>
      <div class="wd-actions">
        <button type="button" @click="loadConfigs" :disabled="Boolean(loading)">
          <RefreshCw :size="16" />刷新配置
        </button>
        <button type="button" @click="createArchive" :disabled="Boolean(loading) || (uploadToObjectStorage && !selectedConfigId)">
          <CloudUpload :size="16" />{{ loading === 'archive' ? '备份中...' : '创建备份' }}
        </button>
      </div>
    </div>

    <p v-if="error" class="wd-error-line">{{ error }}</p>
    <p v-if="notice" class="wd-notice">{{ notice }}</p>

    <section class="wd-log-summary">
      <article class="wd-card wd-stat-card">
        <span>OCI 配置</span>
        <strong><DatabaseBackup :size="20" />{{ configs.length }}</strong>
      </article>
      <article class="wd-card wd-stat-card">
        <span>Bucket</span>
        <strong><Archive :size="20" />{{ buckets.length }}</strong>
      </article>
      <article class="wd-card wd-stat-card">
        <span>归档文件</span>
        <strong>{{ objectCount }}</strong>
      </article>
      <article class="wd-card wd-stat-card">
        <span>归档容量</span>
        <strong>{{ formatBytes(totalSize) }}</strong>
      </article>
    </section>

    <section class="wd-split">
      <article class="wd-card wd-form-card">
        <header><h2><CloudUpload :size="17" /> 备份策略</h2></header>
        <div class="wd-form-grid single">
          <label>
            <span>OCI 配置</span>
            <select v-model="selectedConfigId" @change="selectedBucket = ''; loadBuckets()">
              <option value="">请选择 OCI 配置</option>
              <option v-for="cfg in configs" :key="cfg.id" :value="cfg.id">
                {{ cfg.username || cfg.tenantName || cfg.id }} / {{ cfg.region || '-' }}
              </option>
            </select>
          </label>
          <label>
            <span>Bucket</span>
            <select v-model="selectedBucket" @change="loadObjects">
              <option value="">请选择 Bucket</option>
              <option v-for="bucket in buckets" :key="bucket.name" :value="bucket.name">
                {{ bucket.name }} / {{ bucket.storageTier || 'Standard' }}
              </option>
            </select>
          </label>
          <label>
            <span>归档前缀</span>
            <input v-model="prefix" placeholder="wang-detective/backups" @keyup.enter="loadObjects" />
          </label>
          <label class="wd-switch-row">
            <input v-model="includeLogs" type="checkbox" />
            <span>包含日志目录</span>
          </label>
          <label class="wd-switch-row">
            <input v-model="uploadToObjectStorage" type="checkbox" />
            <span>上传到对象存储</span>
          </label>
          <div class="wd-actions compact">
            <button type="button" @click="loadBuckets" :disabled="!selectedConfigId || Boolean(loading)">
              <RefreshCw :size="16" />刷新 Bucket
            </button>
            <button type="button" class="ghost" @click="loadObjects" :disabled="!selectedBucket || Boolean(loading)">
              刷新归档
            </button>
          </div>
        </div>
      </article>

      <article class="wd-card wd-detail-card">
        <header><h2><Archive :size="17" /> 最近备份结果</h2></header>
        <div v-if="lastBackup" class="wd-detail-summary">
          <div><span>本地路径</span><strong>{{ lastBackup.localPath || '-' }}</strong></div>
          <div><span>对象名称</span><strong>{{ lastBackup.objectName || '未上传' }}</strong></div>
          <div><span>大小</span><strong>{{ formatBytes(lastBackup.sizeBytes) }}</strong></div>
          <div><span>MD5</span><strong>{{ lastBackup.md5 || '-' }}</strong></div>
        </div>
        <div v-else class="wd-placeholder">
          <p>尚未在本页面创建备份。创建后会显示本地路径、对象存储路径和校验信息。</p>
        </div>
      </article>
    </section>

    <section class="wd-split">
      <article class="wd-card wd-table-card">
        <header>
          <h2><RotateCcw :size="17" /> 本地恢复与回滚</h2>
          <button type="button" class="ghost" @click="loadLocalBackups" :disabled="Boolean(loading)">
            <RefreshCw :size="16" />刷新本地备份
          </button>
        </header>
        <div class="wd-form-grid single">
          <label>
            <span>恢复目标备份包</span>
            <select v-model="selectedBackupName" @change="loadRestorePlan">
              <option value="">请选择本地备份包</option>
              <option v-for="item in localBackups" :key="item.name" :value="item.name">
                {{ item.name }} / {{ formatBytes(item.sizeBytes) }} / {{ formatTime(item.modifiedTime) }}
              </option>
            </select>
          </label>
        </div>
        <div v-if="restorePlan" class="wd-placeholder">
          <p>恢复前会保留当前 data、keys、scripts 等目录到回滚目录。建议先在低峰期 SSH 执行命令。</p>
          <pre class="wd-terminal small">{{ restorePlan.command }}</pre>
          <div class="wd-actions compact">
            <button type="button" @click="copyText(restorePlan.command)">复制恢复命令</button>
          </div>
          <ul class="wd-check-list">
            <li v-for="item in restorePlan.warnings" :key="item"><strong>注意</strong><span>{{ item }}</span></li>
          </ul>
        </div>
        <div v-else class="wd-placeholder">
          <p>暂无可恢复的本地备份。先创建一次备份，或把 Object Storage 归档下载到 backups/ 目录。</p>
        </div>
      </article>

      <article class="wd-card wd-form-card">
        <header><h2><Clock :size="17" /> 定时备份与归档策略</h2></header>
        <div class="wd-form-grid single">
          <label>
            <span>Cron 表达式</span>
            <input v-model="cronExpression" placeholder="0 3 * * *" @keyup.enter="loadSchedulePlan" />
          </label>
          <div class="wd-actions compact">
            <button type="button" @click="loadSchedulePlan" :disabled="Boolean(loading)">生成定时方案</button>
            <button type="button" class="ghost" @click="copyText(schedulePlan?.command)" :disabled="!schedulePlan?.command">复制命令</button>
          </div>
        </div>
        <div v-if="schedulePlan" class="wd-placeholder">
          <pre class="wd-terminal small">{{ schedulePlan.command }}</pre>
          <ul class="wd-check-list">
            <li v-for="item in schedulePlan.steps" :key="item"><strong>执行步骤</strong><span>{{ item }}</span></li>
            <li v-for="item in schedulePlan.objectStoragePolicy" :key="item"><strong>归档策略</strong><span>{{ item }}</span></li>
          </ul>
        </div>
      </article>
    </section>

    <article class="wd-card wd-table-card">
      <header>
        <h2><Archive :size="17" /> 对象存储归档</h2>
        <span class="wd-help-line">{{ selectedConfig?.username || '未选择配置' }} / {{ selectedBucket || '未选择 Bucket' }}</span>
      </header>
      <div class="wd-table-scroll">
        <table class="wd-table">
          <thead>
            <tr>
              <th>对象名称</th>
              <th>大小</th>
              <th>创建时间</th>
              <th>MD5</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in objects" :key="item.name">
              <td>{{ item.name }}</td>
              <td>{{ formatBytes(item.sizeBytes) }}</td>
              <td>{{ formatTime(item.timeCreated) }}</td>
              <td>{{ item.md5 || '-' }}</td>
              <td>
                <button type="button" class="wd-link-button" :disabled="loading === `delete:${item.name}`" @click="requestDeleteObject(item)">
                  <Trash2 :size="14" />删除
                </button>
              </td>
            </tr>
            <tr v-if="!objects.length">
              <td colspan="5" class="wd-empty">暂无归档对象，选择 Bucket 后可刷新或创建备份。</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <div v-if="deleteConfirm" class="wd-dialog-backdrop" @click.self="deleteConfirm = null">
      <form class="wd-dialog danger" @submit.prevent="deleteObject">
        <header>
          <div>
            <span>对象存储操作确认</span>
            <h3>删除归档对象</h3>
          </div>
          <button type="button" class="ghost" @click="deleteConfirm = null">关闭</button>
        </header>
        <p>将从 OCI Object Storage 删除这个归档对象。删除后无法从本页面恢复。</p>
        <div class="wd-dialog-target">
          <span>对象名称</span>
          <strong>{{ deleteConfirm.objectName }}</strong>
        </div>
        <footer>
          <button type="button" class="ghost" @click="deleteConfirm = null">取消</button>
          <button type="submit" class="danger" :disabled="loading === `delete:${deleteConfirm.objectName}`">
            {{ loading === `delete:${deleteConfirm.objectName}` ? '删除中...' : '确认删除' }}
          </button>
        </footer>
      </form>
    </div>
  </section>
</template>
