<script setup lang="ts">
defineProps<{
  mode?: 'ops' | 'legacy';
}>();
</script>

<template>
  <section class="wd-card wd-placeholder">
    <h2>{{ mode === 'ops' ? '运维终端' : mode === 'legacy' ? '旧页面迁移中' : '新版功能' }}</h2>
    <p>
      这一页已经进入 Vue 原生化迁移队列。当前线上版本仍使用已修复暗色模式的静态页/内嵌页，
      下一步会把这里替换成完整的 Vue 组件。
    </p>
  </section>
</template>
