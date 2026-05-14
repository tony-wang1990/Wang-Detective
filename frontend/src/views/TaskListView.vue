<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ClipboardList, Play, RefreshCw, Square } from 'lucide-vue-next';
import { apiPost, type PageResult } from '../api/http';

type Row = Record<string, unknown>;

const loading = ref(false);
const keyword = ref('');
const rows = ref<Row[]>([]);
const total = ref(0);
const error = ref('');

const columns = [
  { key: 'id', label: '任务ID' },
  { key: 'cfgName', label: '配置' },
  { key: 'region', label: '区域' },
  { key: 'architecture', label: '架构' },
  { key: 'createNumbers', label: '数量' },
  { key: 'createSuccess', label: '成功' },
  { key: 'running', label: '状态' }
];

function cell(row: Row, key: string) {
  const value = row[key];
  if (value === null || value === undefined || value === '') return '-';
  if (key === 'running') return value ? '运行中' : '已停止';
  return String(value);
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const res = await apiPost<PageResult<Row>>('/oci/createTaskPage', {
      keyword: keyword.value,
      currentPage: 1,
      pageSize: 20
    });
    rows.value = res.data?.records || [];
    total.value = Number(res.data?.total || rows.value.length || 0);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '任务列表读取失败';
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
        <h1>任务列表</h1>
        <p>开机、换 IP、批量任务集中查看，下一阶段继续补齐任务详情和危险操作确认。</p>
      </div>
      <div class="wd-actions">
        <button type="button" @click="load"><RefreshCw :size="16" />刷新</button>
        <button type="button"><Play :size="16" />新建任务</button>
        <button type="button" class="danger"><Square :size="16" />批量停止</button>
      </div>
    </div>

    <div class="wd-card wd-table-card">
      <header>
        <h2><ClipboardList :size="17" /> 开机任务</h2>
        <label class="wd-inline-search">
          <input v-model="keyword" placeholder="搜索任务..." @keyup.enter="load" />
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
            <td :colspan="columns.length">暂无任务数据</td>
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
