<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { basicSetup } from 'codemirror'
import { EditorState, Compartment } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'
import { indentWithTab } from '@codemirror/commands'
import { cpp } from '@codemirror/lang-cpp'
import { oneDark } from '@codemirror/theme-one-dark'

const props = defineProps<{ modelValue: string; template: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const host = ref<HTMLElement | null>(null); const editorRoot = ref<HTMLElement | null>(null)
const fontSize = ref(14); const wrap = ref(true); const fullscreen = ref(false); const feedback = ref('')
const fontConfig = new Compartment(); const wrapConfig = new Compartment()
let editor: EditorView | undefined; let previousOverflow = ''
const fontTheme = () => EditorView.theme({ '&': { fontSize: `${fontSize.value}px` }, '.cm-scroller': { fontFamily: 'Consolas, "Cascadia Code", monospace' } })
watch(() => props.modelValue, value => { if (editor && value !== editor.state.doc.toString()) editor.dispatch({ changes: { from: 0, to: editor.state.doc.length, insert: value } }) })
watch(fontSize, () => editor?.dispatch({ effects: fontConfig.reconfigure(fontTheme()) }))
watch(wrap, () => editor?.dispatch({ effects: wrapConfig.reconfigure(wrap.value ? EditorView.lineWrapping : []) }))
watch(fullscreen, async enabled => {
  if (enabled) { previousOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden' }
  else document.body.style.overflow = previousOverflow
  await nextTick(); editor?.requestMeasure(); editor?.focus()
})
function keyboard(event: KeyboardEvent) {
  if (fullscreen.value && event.key === 'Escape') { fullscreen.value = false; event.preventDefault() }
  if (fullscreen.value && event.key === 'Tab' && !event.defaultPrevented) {
    const controls = Array.from(editorRoot.value?.querySelectorAll<HTMLElement>('button:not(:disabled), select:not(:disabled), input:not(:disabled), [contenteditable="true"]') || []).filter(element => element.getClientRects().length)
    const first = controls[0]; const last = controls.at(-1)
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
  }
}
function reset() { if (window.confirm('重置代码会清空当前代码，并覆盖本题 C++17 草稿。确定清空吗？')) emit('update:modelValue', '') }
function restore() { if (props.modelValue !== props.template && props.modelValue.trim() && !window.confirm('恢复模板会覆盖当前代码和草稿。确定恢复本题 C++17 起始模板吗？')) return; emit('update:modelValue', props.template); feedback.value = '已恢复当前题目的起始模板。' }
async function copyCode() { try { await navigator.clipboard.writeText(props.modelValue); feedback.value = '代码已复制。' } catch { feedback.value = '复制失败，请在编辑器中选择代码后手动复制。' } }
onMounted(() => {
  editor = new EditorView({ parent: host.value!, state: EditorState.create({ doc: props.modelValue, extensions: [basicSetup, cpp(), oneDark, keymap.of([indentWithTab]), fontConfig.of(fontTheme()), wrapConfig.of(EditorView.lineWrapping), EditorView.contentAttributes.of({ 'aria-label': '代码编辑器', 'aria-multiline': 'true' }), EditorView.updateListener.of(update => { if (update.docChanged) emit('update:modelValue', update.state.doc.toString()) })] }) })
  window.addEventListener('keydown', keyboard)
})
onBeforeUnmount(() => { editor?.destroy(); window.removeEventListener('keydown', keyboard); if (fullscreen.value) document.body.style.overflow = previousOverflow })
</script>
<template>
  <section ref="editorRoot" class="oj-code-editor" :class="{ 'oj-editor-fullscreen': fullscreen }" :role="fullscreen ? 'dialog' : undefined" :aria-modal="fullscreen ? true : undefined" :aria-label="fullscreen ? '全屏代码编辑' : '代码编辑区'">
    <div class="oj-editor-toolbar">
      <label class="oj-language">语言<select aria-label="编程语言" value="cpp17"><option value="cpp17">C++17（演示编辑）</option><option disabled>Python 3（规划中）</option><option disabled>Java（规划中）</option></select></label>
      <label>字号<select v-model="fontSize" aria-label="编辑器字号"><option v-for="size in [12,14,16,18,20]" :key="size" :value="size">{{ size }}</option></select></label>
      <label class="oj-checkbox"><input v-model="wrap" type="checkbox" />自动换行</label>
      <button class="oj-button small" @click="copyCode">▣ 复制代码</button>
      <button class="oj-button small" @click="restore">↺ 恢复模板</button>
      <button class="oj-button small danger" @click="reset">⊗ 重置代码</button>
      <button class="oj-button small" :aria-pressed="fullscreen" :aria-label="fullscreen ? '退出全屏编辑' : '全屏编辑'" @click="fullscreen = !fullscreen">{{ fullscreen ? '↙ 退出全屏' : '⛶ 全屏' }}</button>
    </div>
    <div ref="host" class="oj-editor-host"></div>
    <div class="oj-editor-footnote"><span>Tab 缩进 · Shift+Tab 减少缩进 · Ctrl+Z 撤销 · Esc 退出全屏</span><span role="status">{{ feedback }}</span></div>
  </section>
</template>
