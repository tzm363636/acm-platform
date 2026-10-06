<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postData } from '../api/database'
import { currentUser, login, safeReturn } from './auth'
const route = useRoute(), router = useRouter(); const registering = computed(() => route.path === '/register')
const username = ref(''), displayName = ref(''), password = ref(''), confirmPassword = ref(''), busy = ref(false), error = ref(''), notice = ref('')
async function submit() {
  busy.value = true; error.value = ''; notice.value = ''
  try { if (registering.value) { if (password.value !== confirmPassword.value) throw new Error('两次密码输入不一致。'); await postData('/auth/register', { username: username.value, displayName: displayName.value, password: password.value }); notice.value = '注册成功，正在登录…' }
    await login(username.value, password.value); password.value = ''; confirmPassword.value = ''; const target = new URL(safeReturn(new URLSearchParams(location.search).get('return')), location.href); if (target.pathname === location.pathname && target.hash) await router.replace(target.hash.slice(1)); else location.assign(target.href)
  } catch (e) { error.value = (e as Error).message } finally { busy.value = false }
}
</script>
<template>
  <section class="account-card auth-card"><span class="account-eyebrow">ACM CODE SHARE</span><h1>{{ registering ? '创建你的账户' : '欢迎回来' }}</h1><p>记录思路，分享代码，积累每一次成长。</p>
    <p v-if="currentUser" class="account-notice">当前已登录为 {{ currentUser.displayName }}。<RouterLink to="/articles">进入我的文章</RouterLink></p>
    <form @submit.prevent="submit"><label>用户名<input v-model="username" required pattern="[A-Za-z0-9_]{3,32}" maxlength="32" autocomplete="username" placeholder="3–32 位字母、数字或下划线" /></label>
      <label v-if="registering">显示名称<input v-model="displayName" required maxlength="100" autocomplete="nickname" /></label>
      <label>密码<input v-model="password" type="password" required :minlength="registering ? 12 : undefined" :autocomplete="registering ? 'new-password' : 'current-password'" /><small v-if="registering">至少 12 个字符，不超过 72 个 UTF-8 字节。</small></label>
      <label v-if="registering">确认密码<input v-model="confirmPassword" type="password" required autocomplete="new-password" /></label>
      <p class="account-feedback" :class="{ error }" role="status">{{ error || notice }}</p><button class="account-button primary" :disabled="busy">{{ busy ? '正在处理…' : registering ? '注册并登录' : '登录' }}</button>
    </form>
    <p><button class="account-text-button" @click="router.push(registering ? '/login' : '/register')">{{ registering ? '已有账户？前往登录' : '还没有账户？注册' }}</button></p><p class="account-muted">邮箱验证和找回密码暂未开放，请妥善保存密码。</p>
  </section>
</template>
