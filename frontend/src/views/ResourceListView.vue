<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { RefreshCw, UploadCloud, UserRound } from 'lucide-vue-next';
import { apiPost, type PageResult } from '../api/http';

type Row = Record<string, unknown>;

const loading = ref(false);
const keyword = ref('');
const rows = ref<Row[]>([]);
const total = ref(0);
const error = ref('');

const columns = [
  { key: 'cfgName', label: '配置名称' },
  { key: 'userName', label: '用户' },
  { key: 'tenantName', label: '租户' },
  { key: 'region', label: '区域' },
  { key: 'isEnableCreate', label: '开机' }
];

function cell(row: Row, key: string) {
  const value = row[key];
  if (value === null || value === undefined || value === '') return '-';
  if (typeof value === 'boolean') return value ? '是' : '否';
  return String(value);
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const res = await apiPost<PageResult<Row>>('/oci/userPage', {
      keyword: keyword.value,
      currentPage: 1,
      pageSize: 20
    });
    rows.value = res.data?.records || [];
    total.value = Number(res.data?.total || rows.value.length || 0);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '配置列表读取失败';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="wd-page">
    <div class="wd-page-title">
      <div>
        <h1>配置列表</h1>
        <p>OCI API 配置、租户和区域资源入口，后续继续补齐新增/编辑/上传完整表单。</p>
      </div>
      <div class="wd-actions">
        <button type="button" @click="load"><RefreshCw :size="16" />刷新</button>
        <button type="button"><UploadCloud :size="16" />上传配置</button>
      </div>
    </div>

    <div class="wd-card wd-table-card">
      <header>
        <h2><UserRound :size="17" /> OCI 配置</h2>
        <label class="wd-inline-search">
          <input v-model="keyword" placeholder="搜索配置、用户、租户..." @keyup.enter="load" />
          <button type="button" @click="load">查询</button>
        </label>
      </header>
      <p v-if="error" class="wd-error-line">{{ error }}</p>
      <table class="wd-table">
        <thead>
          <tr>
            <th v-for="column in columns" :key="column.key">{{ column.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!loading && rows.length === 0">
            <td :colspan="columns.length">暂无配置数据</td>
          </tr>
          <tr v-for="(row, index) in rows" :key="String(row.id || index)">
            <td v-for="column in columns" :key="column.key">{{ cell(row, column.key) }}</td>
          </tr>
        </tbody>
      </table>
      <footer class="wd-table-footer">共 {{ total }} 条 · 当前显示 {{ rows.length }} 条</footer>
    </div>
  </section>
</template>
