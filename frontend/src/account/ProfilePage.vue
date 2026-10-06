<script setup lang="ts">
import { ref, watch } from 'vue'
import { putData, postData } from '../api/database'
import { currentUser, expireSession, notifyIdentityChange, type User } from './auth'
import { confirmAction } from './confirmation'
import { time } from './types'
const displayName = ref(currentUser.value?.displayName || ''), profileBusy = ref(false), passwordBusy = ref(false)
const profileMessage = ref(''), profileError = ref(''), passwordMessage = ref(''), passwordError = ref('')
const currentPassword = ref(''), newPassword = ref(''), confirmPassword = ref('')
watch(() => currentUser.value?.displayName, name => { if (name) displayName.value = name })
watch(currentUser, user => { if (!user) { currentPassword.value = ''; newPassword.value = ''; confirmPassword.value = '' } })
async function saveProfile() {
  profileBusy.value = true; profileError.value = ''; profileMessage.value = ''
  try { const r = await putData<{ user: User }>('/account/profile', { displayName: displayName.value }); currentUser.value = r.user; notifyIdentityChange(); profileMessage.value = '显示名称已更新，文章作者与导航同步使用此名称。' }
  catch (e) { profileError.value = (e as Error).message } finally { profileBusy.value = false }
}
async function changePassword() {
  passwordError.value = ''; passwordMessage.value = ''
  if (newPassword.value !== confirmPassword.value) { passwordError.value = '两次新密码输入不一致。'; return }
  if (!await confirmAction('修改成功后，这个账号的所有旧登录会话都会失效，需要重新登录。确认修改密码？')) return
  passwordBusy.value = true
  try { const r = await postData<{ message: string }>('/account/password', { currentPassword: currentPassword.value, newPassword: newPassword.value, confirmPassword: confirmPassword.value }); currentPassword.value = ''; newPassword.value = ''; confirmPassword.value = ''; passwordMessage.value = r.message; expireSession(); notifyIdentityChange() }
  catch (e) { passwordError.value = (e as Error).message } finally { passwordBusy.value = false }
}
</script>
<template>
  <header class="account-heading"><div><span class="account-eyebrow">YOUR ACCOUNT</span><h1>个人资料与安全</h1><p>更新显示名称，管理自己的登录密码。</p></div></header>
  <div class="account-profile-grid">
    <section class="account-card"><h2>基本资料</h2><dl class="account-profile"><dt>用户名</dt><dd>{{ currentUser?.username || '会话已结束' }}</dd><dt>角色</dt><dd>{{ currentUser ? currentUser.role === 'ADMIN' ? '管理员' : '普通用户' : '—' }}</dd><dt>注册时间</dt><dd>{{ time(currentUser?.createdAt) }}</dd></dl>
      <form class="account-profile-form" @submit.prevent="saveProfile"><label>显示名称<input v-model="displayName" required maxlength="100" autocomplete="nickname" :disabled="!currentUser || profileBusy" /></label><p class="account-feedback" :class="{ error: profileError }" role="status">{{ profileError || profileMessage }}</p><button class="account-button primary" :disabled="!currentUser || profileBusy">{{ profileBusy ? '保存中…' : '保存显示名称' }}</button></form>
      <p class="account-muted">用户名、角色和文章归属保持不变。OJ 演示记录继续独立统计。</p>
    </section>
    <section class="account-card"><h2>修改本人密码</h2><p class="account-muted">至少 12 个字符，UTF-8 不超过 72 字节。成功后所有旧会话失效。</p>
      <form class="account-profile-form" @submit.prevent="changePassword"><fieldset :disabled="!currentUser || passwordBusy"><label>当前密码<input v-model="currentPassword" type="password" required autocomplete="current-password" /></label><label>新密码<input v-model="newPassword" type="password" required minlength="12" autocomplete="new-password" /></label><label>再次输入新密码<input v-model="confirmPassword" type="password" required minlength="12" autocomplete="new-password" /></label></fieldset><p class="account-feedback" :class="{ error: passwordError }" role="status">{{ passwordError || passwordMessage }}</p><button class="account-button" :disabled="!currentUser || passwordBusy">{{ passwordBusy ? '处理中…' : '确认修改密码' }}</button><RouterLink v-if="passwordMessage" to="/login" class="account-button primary">重新登录</RouterLink></form>
      <p class="account-muted">邮箱、短信和找回密码暂未接入。管理员不能修改其他用户密码。</p>
    </section>
  </div>
</template>

