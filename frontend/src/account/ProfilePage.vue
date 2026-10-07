<script setup lang="ts">
import { ref, watch } from 'vue'
import { getData, putData, postData, ApiError, type ServerPage } from '../api/database'
import { authStatus, currentUser, expireSession, notifyIdentityChange, updateCurrentUser, type User } from './auth'
import { confirmAction } from './confirmation'
import { statusNames, time, type ArticleSummary } from './types'
import PasswordField from './PasswordField.vue'
const displayName = ref(currentUser.value?.displayName || ''), profileBusy = ref(false), passwordBusy = ref(false)
const profileMessage = ref(''), profileError = ref(''), passwordMessage = ref(''), passwordError = ref('')
const currentPassword = ref(''), newPassword = ref(''), confirmPassword = ref('')
watch(() => currentUser.value?.displayName, name => { if (name) displayName.value = name })
watch(() => currentUser.value?.id, () => { currentPassword.value = ''; newPassword.value = ''; confirmPassword.value = ''; profileMessage.value = ''; passwordMessage.value = '' })
const recent = ref<ArticleSummary[]>([]), recentLoading = ref(false), recentError = ref('')
async function loadRecent() { const owner = currentUser.value?.id; if (!owner || authStatus.value !== 'authenticated') return; recentLoading.value = true; recentError.value = ''; try { const r = await getData<ServerPage<ArticleSummary>>('/account/articles', { size: 3 }); if (currentUser.value?.id === owner && authStatus.value === 'authenticated') recent.value = r.items } catch (e) { if (currentUser.value?.id === owner) recentError.value = (e as Error).message } finally { recentLoading.value = false } }
watch([authStatus, () => currentUser.value?.id], ([status]) => { if (status === 'authenticated') void loadRecent() }, { immediate: true })
async function saveProfile() {
  if (profileBusy.value || authStatus.value !== 'authenticated') return
  profileBusy.value = true; profileError.value = ''; profileMessage.value = ''
  try { const r = await putData<{ user: User }>('/account/profile', { displayName: displayName.value }); if (!updateCurrentUser(r.user)) throw new ApiError('账户已变化，请重新确认身份。'); profileMessage.value = '显示名称已更新，文章作者与导航同步使用此名称。' }
  catch (e) { profileError.value = (e as Error).message } finally { profileBusy.value = false }
}
async function changePassword() {
  if (passwordBusy.value || authStatus.value !== 'authenticated') return
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
      <form class="account-profile-form" @submit.prevent="saveProfile"><label>显示名称<input v-model="displayName" required maxlength="100" autocomplete="nickname" :disabled="authStatus !== 'authenticated' || profileBusy" /></label><p class="account-feedback" :class="{ error: profileError }" role="status">{{ profileError || profileMessage }}</p><button class="account-button primary" :disabled="authStatus !== 'authenticated' || profileBusy">{{ profileBusy ? '保存中…' : '保存显示名称' }}</button></form>
      <p class="account-muted">用户名、角色和文章归属保持不变。OJ 演示记录继续独立统计。</p>
    </section>
    <section class="account-card"><h2>修改本人密码</h2><p class="account-muted">至少 12 个字符，UTF-8 不超过 72 字节。成功后所有旧会话失效。</p>
      <form class="account-profile-form" @submit.prevent="changePassword"><fieldset :disabled="authStatus !== 'authenticated' || passwordBusy"><PasswordField v-model="currentPassword" label="当前密码" autocomplete="current-password" :disabled="authStatus !== 'authenticated' || passwordBusy" /><PasswordField v-model="newPassword" label="新密码" :minlength="12" autocomplete="new-password" :disabled="authStatus !== 'authenticated' || passwordBusy" /><PasswordField v-model="confirmPassword" label="再次输入新密码" :minlength="12" autocomplete="new-password" :disabled="authStatus !== 'authenticated' || passwordBusy" /></fieldset><p class="account-feedback" :class="{ error: passwordError }" role="status">{{ passwordError || passwordMessage }}</p><button class="account-button" :disabled="authStatus !== 'authenticated' || passwordBusy">{{ passwordBusy ? '处理中…' : '确认修改密码' }}</button><RouterLink v-if="passwordMessage" to="/login" class="account-button primary">重新登录</RouterLink></form>
      <p class="account-muted">邮箱、短信和找回密码暂未接入。管理员不能修改其他用户密码。</p>
    </section>
  </div>
  <section class="account-card account-recent"><div class="account-row"><h2>继续写作与投稿进度</h2><RouterLink to="/articles">我的文章</RouterLink></div><p>草稿、最新审核状态和驳回原因均来自你的文章记录。</p><p class="account-feedback" role="status">{{ recentLoading ? '正在读取最近文章…' : recentError }}<button v-if="recentError" class="account-text-button" @click="loadRecent">重试</button></p><div v-if="!recentLoading && !recentError && !recent.length"><p>还没有文章，从一份草稿开始。</p><RouterLink class="account-button primary" to="/articles/new">开始写作</RouterLink></div><article v-for="item in recent" :key="item.id"><span class="account-badge" :data-status="item.status">{{ statusNames[item.status] }}</span><h3><RouterLink :to="`/articles/${item.id}`">{{ item.title }}</RouterLink></h3><p v-if="item.rejectionReason">驳回原因：{{ item.rejectionReason }}</p><small>更新于 {{ time(item.updatedAt) }}</small><RouterLink class="account-button" :to="`/articles/${item.id}`">{{ ['DRAFT', 'REJECTED'].includes(item.status) ? '继续写作' : '查看投稿' }}</RouterLink></article></section>
</template>

