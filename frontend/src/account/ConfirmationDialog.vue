<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { confirmation, answerConfirmation } from './confirmation'
const dialog = ref<HTMLElement>()
watch(confirmation, async value => { document.getElementById('app')?.toggleAttribute('inert', !!value); if (value) { await nextTick(); dialog.value?.querySelector<HTMLButtonElement>('button')?.focus() } })
onBeforeUnmount(() => document.getElementById('app')?.removeAttribute('inert'))
function key(event: KeyboardEvent) { if (event.key === 'Escape') { event.preventDefault(); answerConfirmation('cancel') } else if (event.key === 'Tab') { const buttons = dialog.value?.querySelectorAll<HTMLButtonElement>('button'); if (!buttons?.length) return; const first = buttons[0]!, last = buttons[buttons.length - 1]!; if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() } else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() } } }
</script>
<template><Teleport to="body"><div v-if="confirmation" class="account-confirm-overlay" @keydown="key"><section ref="dialog" class="account-card account-confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="account-confirm-title" aria-describedby="account-confirm-message"><h2 id="account-confirm-title">确认操作</h2><p id="account-confirm-message">{{ confirmation.message }}</p><div><button v-for="option in confirmation.options" :key="option.value" class="account-button" :class="{ primary: option.value === 'confirm' || option.value === 'save' }" @click="answerConfirmation(option.value)">{{ option.label }}</button></div></section></div></Teleport></template>
