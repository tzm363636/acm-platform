<script setup lang="ts">
withDefaults(defineProps<{ label: string; rows?: number; detail?: boolean }>(), { rows: 8, detail: false })
</script>
<template>
  <div class="oj-skeleton" :class="{ 'oj-skeleton-detail': detail }" role="status">
    <span class="visually-hidden">{{ label }}</span>
    <div v-if="detail" class="oj-skeleton-intro" aria-hidden="true"><i></i><i></i></div>
    <div v-if="detail" class="oj-skeleton-panes" aria-hidden="true"><div><i v-for="n in 7" :key="n"></i></div><div><i v-for="n in 10" :key="n"></i></div></div>
    <div v-else aria-hidden="true"><div class="oj-skeleton-header"><i></i><i></i><i></i></div><div v-for="n in rows" :key="n" class="oj-skeleton-row"><i></i><i></i><i></i><i></i></div></div>
  </div>
</template>
