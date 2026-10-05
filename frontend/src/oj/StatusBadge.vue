<script setup lang="ts">
import { verdicts, type Verdict } from './types'
defineProps<{ verdict: Verdict }>()
</script>
<template><span class="oj-badge" :class="`tone-${verdicts[verdict].tone}`"><span aria-hidden="true">{{ verdicts[verdict].icon }}</span> {{ verdict === 'SystemError' ? '' : verdict }} {{ verdicts[verdict].label }}</span></template>
