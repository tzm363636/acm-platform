<script setup lang="ts">
const paths = {
  book: ['M12 5c-3-2-6-2-9-1v16c3-1 6-1 9 1 3-2 6-2 9-1V4c-3-1-6-1-9 1Z', 'M12 5v16'],
  user: ['M8 7a4 4 0 1 0 8 0a4 4 0 1 0-8 0', 'M4 21v-2a8 8 0 0 1 16 0v2'],
  copy: ['M9 9h12v12H9Z', 'M15 5V3H3v12h2'],
  reset: ['M3 10a9 9 0 1 1 2 9', 'M3 3v7h7'],
  trash: ['M3 6h18', 'M9 6V3h6v3', 'M5 6l1 15h12l1-15', 'M10 10v7m4-7v7'],
  expand: ['M8 3H3v5m13-5h5v5M3 16v5h5m13-5v5h-5'],
  play: ['m8 4 13 8-13 8Z'],
  send: ['m22 2-7 20-4-9-9-4Z', 'M22 2 11 13'],
  list: ['M8 5h13M8 12h13M8 19h13M3 5h.01M3 12h.01M3 19h.01'],
  globe: ['M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0', 'M3 12h18', 'M12 3c5 5 5 13 0 18-5-5-5-13 0-18'],
  lightbulb: ['M9 18h6m-6 3h6', 'M8 14a6 6 0 1 1 8 0c-1 1-1 2-1 4H9c0-2 0-3-1-4Z'],
  spark: ['m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5Z'],
  chevron: ['m6 9 6 6 6-6'],
} as const
defineProps<{ name: keyof typeof paths }>()
</script>
<template><svg class="ui-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path v-for="path in paths[name]" :key="path" :d="path" /></svg></template>
<style scoped>.ui-icon { display: inline-block; width: 1.15em; height: 1.15em; flex: none; vertical-align: -.15em; }</style>
