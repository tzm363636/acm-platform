<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { confirmation, answerConfirmation } from './confirmation'
const cancel = ref<HTMLButtonElement>(), accept = ref<HTMLButtonElement>()
watch(confirmation, async value => { document.getElementById('app')?.toggleAttribute('inert', !!value); if (value) { await nextTick(); cancel.value?.focus() } })
onBeforeUnmount(() => document.getElementById('app')?.removeAttribute('inert'))
function key(event: KeyboardEvent) { if (event.key === 'Escape') { event.preventDefault(); answerConfirmation(false) } else if (event.key === 'Tab') { if (event.shiftKey && document.activeElement === cancel.value) { event.preventDefault(); accept.value?.focus() } else if (!event.shiftKey && document.activeElement === accept.value) { event.preventDefault(); cancel.value?.focus() } } }
</script>
<template><Teleport to="body"><div v-if="confirmation" class="account-confirm-overlay" @keydown="key"><section class="account-card account-confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="account-confirm-title" aria-describedby="account-confirm-message"><h2 id="account-confirm-title">确认操作</h2><p id="account-confirm-message">{{ confirmation.message }}</p><div><button ref="cancel" class="account-button" @click="answerConfirmation(false)">取消</button><button ref="accept" class="account-button primary" @click="answerConfirmation(true)">确认</button></div></section></div></Teleport></template>
