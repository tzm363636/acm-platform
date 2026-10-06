<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import hljs from 'highlight.js/lib/core'
import cpp from 'highlight.js/lib/languages/cpp'
import { codeLanguage } from './codeLanguages'

hljs.registerLanguage('cpp', cpp)

const props = defineProps<{ code: string; label: string; language?: string | null }>()
const language = computed(() => codeLanguage(props.language))
const loaders: Record<string, () => Promise<{ default: any }>> = {
  python: () => import('highlight.js/lib/languages/python'), java: () => import('highlight.js/lib/languages/java'),
  javascript: () => import('highlight.js/lib/languages/javascript'), sql: () => import('highlight.js/lib/languages/sql'),
}
const ready = ref(0)
watch(() => language.value.value, async value => { if (loaders[value] && !hljs.getLanguage(value)) { try { hljs.registerLanguage(value, (await loaders[value]!()).default); ready.value++ } catch { /* Plain-text fallback preserves source. */ } } }, { immediate: true })
const highlightedCode = computed(() => { void ready.value; const value = language.value.value; return value !== 'plaintext' && hljs.getLanguage(value) ? hljs.highlight(props.code, { language: value }).value : props.code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') })
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
      <span>{{ language.label }}</span>
      <button type="button" :aria-label="`复制${label}代码`" :disabled="copyState === 'copying'" @click="copyCode">
        <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="8" y="8" width="12" height="13" rx="2"/><path d="M16 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h3"/></svg>
        {{ copyState === 'copied' ? '已复制 ✓' : copyState === 'copying' ? '复制中…' : '复制代码' }}
      </button>
    </div>
    <!-- highlight.js escapes source text before adding its syntax markup. -->
    <pre tabindex="0" :aria-label="`${label}，${language.label}代码`"><code :class="['hljs', `language-${language.value}`]" v-html="highlightedCode"></code></pre>
    <p class="reading-copy-status" role="status" aria-live="polite">{{ copyState === 'copied' ? '代码已复制到剪贴板' : copyState === 'failed' ? '复制失败，请选择代码手动复制。' : '' }}</p>
  </div>
</template>
