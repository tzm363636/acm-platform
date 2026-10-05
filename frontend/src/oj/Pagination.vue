<script setup lang="ts">
defineProps<{ page: number; pages: number; total: number; start: number; end: number; size: number }>()
defineEmits<{ page: [value: number]; size: [value: number] }>()
</script>
<template>
  <div class="oj-pagination">
    <span>共 {{ total }} 条，当前显示 {{ start }}–{{ end }}</span>
    <nav aria-label="分页"><button class="oj-button small" :disabled="page <= 1" aria-label="上一页" @click="$emit('page', page - 1)">‹</button><span aria-live="polite">{{ page }} / {{ pages }}</span><button class="oj-button small" :disabled="page >= pages" aria-label="下一页" @click="$emit('page', page + 1)">›</button></nav>
    <label>每页显示 <select :value="size" @change="$emit('size', Number(($event.target as HTMLSelectElement).value))"><option :value="4">4 条</option><option :value="8">8 条</option><option :value="16">16 条</option></select></label>
  </div>
</template>
