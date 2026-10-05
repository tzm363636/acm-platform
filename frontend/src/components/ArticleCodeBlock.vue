<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import hljs from 'highlight.js/lib/core'
import cpp from 'highlight.js/lib/languages/cpp'

hljs.registerLanguage('cpp', cpp)

const props = defineProps<{ code: string; label: string }>()
const highlightedCode = computed(() => hljs.highlight(props.code, { language: 'cpp' }).value)
const copyState = ref<'idle' | 'copying' | 'copied' | 'failed'>('idle')
let resetTimer: ReturnType<typeof setTimeout> | undefined

async function copyCode() {
  if (resetTimer) clearTimeout(resetTimer)
  copyState.value = 'copying'
  try {
    await navigator.clipboard.writeText(props.code)
    copyState.value = 'copied'
    resetTimer = setTimeout(() => { copyState.value = 'idle' }, 2500)
  } catch {
    copyState.value = 'failed'
  }
}

onBeforeUnmount(() => { if (resetTimer) clearTimeout(resetTimer) })
</script>

<template>
  <div class="reading-code-block">
    <div class="reading-code-toolbar">
      <span>C++17</span>
      <button type="button" :aria-label="`复制${label}代码`" :disabled="copyState === 'copying'" @click="copyCode">
        <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="8" y="8" width="12" height="13" rx="2"/><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3"/></svg>
        {{ copyState === 'copied' ? '已复制 ✓' : copyState === 'copying' ? '复制中…' : '复制代码' }}
      </button>
    </div>
    <!-- highlight.js escapes source text before adding its syntax markup. -->
    <pre tabindex="0" :aria-label="`${label}，C++17 代码`"><code class="hljs language-cpp" v-html="highlightedCode"></code></pre>
    <p class="reading-copy-status" role="status" aria-live="polite">{{ copyState === 'copied' ? '代码已复制到剪贴板' : copyState === 'failed' ? '复制失败，请选择代码手动复制。' : '' }}</p>
  </div>
</template>
