<script setup lang="ts">
import { ref, watch } from 'vue'
const props = defineProps<{ modelValue: string; label: string; autocomplete: 'current-password' | 'new-password'; minlength?: number; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const visible = ref(false), capsLock = ref(false)
function keys(event: KeyboardEvent) { capsLock.value = typeof event.getModifierState === 'function' && event.getModifierState('CapsLock') }
watch(() => props.disabled, disabled => { if (disabled) { visible.value = false; capsLock.value = false } })
</script>
<template>
  <label class="account-password-label">{{ label }}<span class="account-password-field"><input :value="modelValue" :aria-label="label" :type="visible ? 'text' : 'password'" required :minlength="minlength" :autocomplete="autocomplete" :disabled="disabled" @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)" @keydown="keys" @keyup="keys" @blur="capsLock = false" /><button type="button" :disabled="disabled" :aria-label="`${visible ? '隐藏' : '显示'}${label}`" :aria-pressed="visible" @click="visible = !visible">{{ visible ? '隐藏' : '显示' }}</button></span><small class="account-caps-warning" role="status">{{ capsLock ? 'Caps Lock 已开启，请注意大小写。' : '' }}</small></label>
</template>
