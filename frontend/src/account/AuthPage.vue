<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postData } from '../api/database'
import { authStatus, authBusy, authIssue, currentUser, loadUser, login, safeReturn } from './auth'
import PasswordField from './PasswordField.vue'
const route = useRoute(), router = useRouter(); const registering = computed(() => route.path === '/register')
const username = ref(''), displayName = ref(''), password = ref(''), confirmPassword = ref(''), busy = ref(false), error = ref(''), notice = ref('')
const registeredUsername = ref('')
const destination = computed(() => safeReturn(new URLSearchParams(location.search).get('return')))
async function submit() {
  if (busy.value || authBusy.value) return
  busy.value = true; error.value = ''; notice.value = ''
  try { if (registering.value) { if (password.value !== confirmPassword.value) throw new Error('两次密码输入不一致。'); if (registeredUsername.value !== username.value) { await postData('/auth/register', { username: username.value, displayName: displayName.value, password: password.value }); registeredUsername.value = username.value }; notice.value = '账户已创建，正在登录…' }
    await login(username.value, password.value); password.value = ''; confirmPassword.value = ''; const target = new URL(destination.value, location.href); if (target.pathname === location.pathname && target.search === location.search && target.hash) await router.replace(target.hash.slice(1)); else location.assign(target.href)
  } catch (e) { error.value = (registering.value && registeredUsername.value === username.value ? '账户已创建，登录尚未完成。' : '') + (e as Error).message; notice.value = '' } finally { busy.value = false }
}
</script>
<template>
  <section class="account-card auth-card"><span class="account-eyebrow">ACM CODE SHARE</span><h1>{{ registering ? '创建你的账户' : '欢迎回来' }}</h1><p>记录思路，分享代码，积累每一次成长。</p>
    <p v-if="authStatus === 'authenticated' && currentUser" class="account-notice">当前已登录为 {{ currentUser.displayName }}。<a :href="destination">继续访问</a> · <RouterLink to="/articles">我的文章</RouterLink></p>
    <p v-else-if="authStatus === 'checking'" class="account-notice" role="status">正在确认登录状态…</p>
    <div v-else-if="authStatus === 'unavailable'" class="account-notice" role="status">{{ authIssue }}<button type="button" class="account-text-button" @click="loadUser(true)">重新检查账户服务</button></div>
    <form v-if="authStatus !== 'authenticated'" @submit.prevent="submit"><label>用户名<input v-model="username" required pattern="[A-Za-z0-9_]{3,32}" maxlength="32" autocomplete="username" placeholder="3–32 位字母、数字或下划线" /></label>
      <label v-if="registering">显示名称<input v-model="displayName" required maxlength="100" autocomplete="nickname" /></label>
      <PasswordField v-model="password" label="密码" :minlength="registering ? 12 : undefined" :autocomplete="registering ? 'new-password' : 'current-password'" :disabled="busy || authBusy" /><small v-if="registering">至少 12 个字符，不超过 72 个 UTF-8 字节。</small>
      <PasswordField v-if="registering" v-model="confirmPassword" label="确认密码" :minlength="12" autocomplete="new-password" :disabled="busy || authBusy" />
      <p class="account-feedback" :class="{ error }" role="status">{{ error || notice }}</p><button class="account-button primary" :disabled="busy || authBusy || authStatus === 'checking'">{{ busy ? '正在处理…' : registering ? '注册并登录' : '登录' }}</button>
    </form>
    <p v-if="authStatus !== 'authenticated'"><button class="account-text-button" :disabled="busy || authBusy" @click="router.push(registering ? '/login' : '/register')">{{ registering ? '已有账户？前往登录' : '还没有账户？注册' }}</button></p><p class="account-muted">邮箱验证和找回密码暂未开放，请妥善保存密码。</p>
  </section>
</template>
