<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { CheckCircle2, ExternalLink, RefreshCw, ShieldCheck, Trash2, UploadCloud, UserRound } from 'lucide-vue-next';
import { apiForm, apiPost, type PageResult } from '../api/http';

type Row = {
  id?: string;
  username?: string;
  cfgName?: string;
  userName?: string;
  tenantName?: string;
  region?: string;
  regionName?: string;
  enableCreate?: number;
  isEnableCreate?: boolean;
  createTime?: string;
  [key: string]: unknown;
};

const loading = ref(false);
const keyword = ref('');
const rows = ref<Row[]>([]);
const total = ref(0);
const error = ref('');
const notice = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const isEnableCreate = ref<string>('');
const selectedIds = ref<string[]>([]);
const selectedDetail = ref<Row | null>(null);
const detailLoading = ref(false);
const showAddForm = ref(false);
const keyFile = ref<File | null>(null);
const addForm = reactive({
  username: '',
  ociCfgStr: ''
});
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));
const selectedCount = computed(() => selectedIds.value.length);

const columns = [
  { key: 'username', label: '配置名称' },
  { key: 'tenantName', label: '租户' },
  { key: 'region', label: '区域' },
  { key: 'enableCreate', label: '开机任务' },
  { key: 'createTime', label: '创建时间' }
];

function rowId(row: Row) {
  return String(row.id || '');
}

function displayName(row: Row) {
  return String(row.username || row.cfgName || row.userName || '-');
}

function cell(row: Row, key: string) {
  if (key === 'username') return displayName(row);
  if (key === 'region') {
    const region = row.region || '-';
    return row.regionName ? `${region} (${row.regionName})` : String(region);
  }
  if (key === 'enableCreate') {
    const enabled = Number(row.enableCreate ?? 0) === 1 || row.isEnableCreate === true;
    return enabled ? '执行中' : '无任务';
  }
  const value = row[key];
  if (value === null || value === undefined || value === '') return '-';
  if (typeof value === 'boolean') return value ? '是' : '否';
  return String(value);
}

function statusClass(row: Row) {
  const enabled = Number(row.enableCreate ?? 0) === 1 || row.isEnableCreate === true;
  return enabled ? 'success' : 'muted';
}

function toggleRow(row: Row) {
  const id = rowId(row);
  if (!id) return;
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter((item) => item !== id)
    : [...selectedIds.value, id];
}

function toggleAll(checked: boolean) {
  selectedIds.value = checked ? rows.value.map(rowId).filter(Boolean) : [];
}

function resetPageAndLoad() {
  currentPage.value = 1;
  load();
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const res = await apiPost<PageResult<Row>>('/oci/userPage', {
      keyword: keyword.value,
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      isEnableCreate: isEnableCreate.value === '' ? null : Number(isEnableCreate.value)
    });
    rows.value = res.data?.records || [];
    total.value = Number(res.data?.total || rows.value.length || 0);
    selectedIds.value = selectedIds.value.filter((id) => rows.value.some((row) => rowId(row) === id));
    notice.value = `已刷新：${new Date().toLocaleTimeString()}`;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '配置列表读取失败';
  } finally {
    loading.value = false;
  }
}

async function checkAlive() {
  loading.value = true;
  try {
    const res = await apiPost<unknown>('/oci/checkAlive', {});
    notice.value = res.msg || '测活完成';
    await load();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '测活失败';
  } finally {
    loading.value = false;
  }
}

function onKeyFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  keyFile.value = input.files?.[0] || null;
}

async function addConfig() {
  if (!addForm.username.trim() || !addForm.ociCfgStr.trim() || !keyFile.value) {
    error.value = '请填写配置名称、OCI config 内容并选择私钥文件';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const form = new FormData();
    form.append('username', addForm.username.trim());
    form.append('ociCfgStr', addForm.ociCfgStr.trim());
    form.append('file', keyFile.value);
    const res = await apiForm<void>('/oci/addCfg', form);
    notice.value = res.msg || '配置已新增';
    addForm.username = '';
    addForm.ociCfgStr = '';
    keyFile.value = null;
    showAddForm.value = false;
    await load();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '新增配置失败';
  } finally {
    loading.value = false;
  }
}

async function renameConfig(row: Row) {
  const id = rowId(row);
  if (!id) return;
  const nextName = window.prompt('请输入新的配置名称', displayName(row));
  if (!nextName || nextName === displayName(row)) return;
  try {
    await apiPost('/oci/updateCfgName', { cfgId: id, updateCfgName: nextName });
    notice.value = '配置名称已修改';
    await load();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '修改配置名称失败';
  }
}

async function deleteSelected() {
  if (!selectedIds.value.length) return;
  if (!window.confirm(`确认删除 ${selectedIds.value.length} 个配置？`)) return;
  try {
    await apiPost('/oci/removeCfg', { idList: selectedIds.value });
    notice.value = '配置已删除';
    selectedIds.value = [];
    await load();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除配置失败';
  }
}

async function stopCreate(row: Row) {
  const id = rowId(row);
  if (!id || !window.confirm(`确认停止 ${displayName(row)} 的开机任务？`)) return;
  try {
    await apiPost('/oci/stopCreate', { userId: id });
    notice.value = '停止开机任务已提交';
    await load();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '停止开机任务失败';
  }
}

async function releaseSecurityRule(row: Row) {
  const id = rowId(row);
  if (!id || !window.confirm(`确认放行 ${displayName(row)} 的安全列表？`)) return;
  try {
    await apiPost('/oci/releaseSecurityRule', { ociCfgId: id });
    notice.value = '安全列表放行任务已提交';
    await load();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '安全列表放行失败';
  }
}

async function openDetails(row: Row) {
  const id = rowId(row);
  if (!id) return;
  selectedDetail.value = row;
  detailLoading.value = true;
  error.value = '';
  try {
    const res = await apiPost<Record<string, unknown>>('/oci/details', {
      cfgId: id,
      cleanReLaunchDetails: true
    });
    selectedDetail.value = {
      ...row,
      liveDetails: res.data || {}
    };
    notice.value = '已读取 OCI 实时详情';
  } catch (err) {
    error.value = err instanceof Error ? err.message : '读取 OCI 实时详情失败';
  } finally {
    detailLoading.value = false;
  }
}

function previousPage() {
  if (currentPage.value > 1) {
    currentPage.value -= 1;
    load();
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value += 1;
    load();
  }
}

onMounted(load);
</script>

<template>
  <section class="wd-page">
    <div class="wd-page-title">
      <div>
        <h1>配置列表</h1>
        <p>OCI API 配置、租户和区域资源入口，已补齐分页、筛选、测活、改名、删除和常用操作。</p>
      </div>
      <div class="wd-actions">
        <button type="button" @click="load"><RefreshCw :size="16" />刷新</button>
        <button type="button" @click="checkAlive"><CheckCircle2 :size="16" />一键测活</button>
        <button type="button" class="ghost" @click="showAddForm = !showAddForm"><UploadCloud :size="16" />新增配置</button>
        <button type="button" class="danger" :disabled="selectedCount === 0" @click="deleteSelected">
          <Trash2 :size="16" />删除 {{ selectedCount || '' }}
        </button>
      </div>
    </div>

    <div v-if="showAddForm" class="wd-card wd-form-card">
      <header>
        <h2><UploadCloud :size="17" /> 新增 OCI 配置</h2>
        <button type="button" class="ghost" @click="showAddForm = false">收起</button>
      </header>
      <div class="wd-form-grid">
        <label>
          <span>配置名称</span>
          <input v-model="addForm.username" placeholder="例如 oracle-seoul-a1" />
        </label>
        <label>
          <span>私钥文件</span>
          <input type="file" @change="onKeyFileChange" />
        </label>
        <label class="wide">
          <span>OCI config 内容</span>
          <textarea v-model="addForm.ociCfgStr" placeholder="[DEFAULT]&#10;user=ocid1.user...&#10;fingerprint=...&#10;tenancy=ocid1.tenancy...&#10;region=ap-seoul-1"></textarea>
        </label>
      </div>
      <div class="wd-actions compact">
        <button type="button" :disabled="loading" @click="addConfig"><UploadCloud :size="16" />提交并校验</button>
      </div>
    </div>

    <div class="wd-card wd-table-card">
      <header>
        <h2><UserRound :size="17" /> OCI 配置</h2>
        <div class="wd-table-tools">
          <label class="wd-inline-search">
            <input v-model="keyword" placeholder="搜索配置、用户、租户..." @keyup.enter="resetPageAndLoad" />
            <button type="button" @click="resetPageAndLoad">查询</button>
          </label>
          <select v-model="isEnableCreate" @change="resetPageAndLoad">
            <option value="">全部开机状态</option>
            <option value="1">执行开机任务中</option>
            <option value="0">无开机任务</option>
          </select>
        </div>
      </header>
      <p v-if="error" class="wd-error-line">{{ error }}</p>
      <p v-else-if="notice" class="wd-muted-line">{{ notice }}</p>
      <table class="wd-table">
        <thead>
          <tr>
            <th><input type="checkbox" :checked="rows.length > 0 && selectedCount === rows.length" @change="toggleAll(($event.target as HTMLInputElement).checked)" /></th>
            <th v-for="column in columns" :key="column.key">{{ column.label }}</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td :colspan="columns.length + 2">加载中...</td>
          </tr>
          <tr v-else-if="rows.length === 0">
            <td :colspan="columns.length + 2">暂无配置数据</td>
          </tr>
          <tr v-for="(row, index) in rows" v-else :key="String(row.id || index)">
            <td><input type="checkbox" :checked="selectedIds.includes(rowId(row))" @change="toggleRow(row)" /></td>
            <td v-for="column in columns" :key="column.key">
              <span v-if="column.key === 'enableCreate'" class="wd-badge" :class="statusClass(row)">{{ cell(row, column.key) }}</span>
              <span v-else>{{ cell(row, column.key) }}</span>
            </td>
            <td>
              <div class="wd-row-actions">
                <button type="button" @click="openDetails(row)"><ExternalLink :size="14" />实时详情</button>
                <button type="button" @click="renameConfig(row)">改名</button>
                <button type="button" @click="releaseSecurityRule(row)"><ShieldCheck :size="14" />放行</button>
                <button type="button" :disabled="Number(row.enableCreate || 0) === 0" @click="stopCreate(row)">停止</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <footer class="wd-table-footer wd-pager">
        <span>共 {{ total }} 条 · 第 {{ currentPage }} / {{ totalPages }} 页</span>
        <select v-model.number="pageSize" @change="resetPageAndLoad">
          <option :value="10">10 条/页</option>
          <option :value="20">20 条/页</option>
          <option :value="50">50 条/页</option>
        </select>
        <button type="button" :disabled="currentPage <= 1" @click="previousPage">上一页</button>
        <button type="button" :disabled="currentPage >= totalPages" @click="nextPage">下一页</button>
      </footer>
    </div>

    <div v-if="selectedDetail" class="wd-card wd-detail-card">
      <header>
        <h2>OCI 实时详情</h2>
        <span v-if="detailLoading">读取 OCI 中...</span>
        <button type="button" @click="selectedDetail = null">关闭</button>
      </header>
      <pre class="wd-terminal small">{{ JSON.stringify(selectedDetail, null, 2) }}</pre>
    </div>
  </section>
</template>
