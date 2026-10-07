<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Navbar from '../components/Navbar.vue'
import ConfirmationDialog from './ConfirmationDialog.vue'
import { authStatus, authReady, authIssue, currentUser, loadUser, loginUrl, sessionExpired } from './auth'
const route = useRoute(); const error = ref('')
const publicPage = computed(() => ['/login', '/register'].includes(route.path))
const adminPage = computed(() => route.path.startsWith('/admin'))
const authorized = computed(() => authStatus.value === 'authenticated' && !!currentUser.value && (!adminPage.value || currentUser.value.role === 'ADMIN'))
const openedEditor = ref('')
const openedOwner = ref<number>()
const retainedEditor = computed(() => openedEditor.value === route.path)
const differentOwner = computed(() => retainedEditor.value && !!currentUser.value && currentUser.value.id !== openedOwner.value)
watch([authorized, () => route.path], () => {
  const retainsInput = /^\/(admin\/)?articles\/(new|\d+)$/.test(route.path) || route.path === '/profile'
  if (!retainsInput) { openedEditor.value = ''; openedOwner.value = undefined; return }
  if (openedEditor.value && openedEditor.value !== route.path) { openedEditor.value = ''; openedOwner.value = undefined }
  if (authorized.value && !openedEditor.value) { openedEditor.value = route.path; openedOwner.value = currentUser.value!.id }
}, { immediate: true })
async function retry() { error.value = ''; try { await loadUser(true) } catch { error.value = '账户服务暂不可用，请重试。' } }
onMounted(retry)
</script>
<template>
  <Navbar active-page="account" />
  <ConfirmationDialog />
  <main class="account-page"><div class="account-shell">
    <nav class="account-nav" aria-label="用户中心"><RouterLink to="/articles">我的文章</RouterLink><RouterLink to="/profile">个人资料</RouterLink><RouterLink v-if="authStatus === 'authenticated' && currentUser?.role === 'ADMIN'" to="/admin">管理员控制台</RouterLink></nav>
    <div v-if="sessionExpired" class="account-notice" role="alert">会话已过期。输入会保留，请<a :href="loginUrl()">重新登录</a>后重试。</div>
    <div v-if="authStatus === 'unavailable' && retainedEditor" class="account-notice" role="alert">{{ authIssue }} 输入已保留，保存已暂停。<button class="account-text-button" @click="retry">重新检查</button></div>
    <div v-if="authStatus === 'checking' && retainedEditor" class="account-notice" role="status">正在确认账号身份，输入已保留，保存已暂停。</div>
    <div v-if="authStatus === 'anonymous' && retainedEditor" class="account-notice" role="status">你已退出登录，当前输入已保留。<a :href="loginUrl()">重新登录原账号</a>后可以继续。</div>
    <div v-if="differentOwner" class="account-card account-empty" role="status"><h1>登录账号已变化</h1><p>原账号的未保存内容已保留，当前账号不能查看或提交这份内容。重新登录原账号可继续；也可以返回当前账号的文章列表。</p><RouterLink class="account-button" to="/articles">返回我的文章</RouterLink></div>
    <div v-if="publicPage || authorized || retainedEditor" v-show="!differentOwner && (publicPage || !['checking', 'anonymous'].includes(authStatus))"><RouterView :key="route.path" /></div>
    <section v-if="!publicPage && !authorized && !retainedEditor" class="account-card account-empty" :aria-busy="!authReady">
      <h1>{{ !authReady ? '正在确认登录状态…' : authIssue ? '账户服务暂不可用' : currentUser ? '没有管理员权限' : '请先登录' }}</h1><p>{{ error || authIssue || (currentUser ? '此控制台仅管理员可访问。' : '登录后可以保存草稿和提交文章审核。') }}</p><a v-if="authReady && !currentUser && !authIssue" class="account-button primary" :href="loginUrl()">前往登录</a><RouterLink v-else-if="currentUser" class="account-button" to="/articles">返回我的文章</RouterLink><button class="account-button" @click="retry">重新检查</button>
    </section>
  </div></main>
</template>
