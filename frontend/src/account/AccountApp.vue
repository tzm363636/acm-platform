<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Navbar from '../components/Navbar.vue'
import ConfirmationDialog from './ConfirmationDialog.vue'
import { authReady, authIssue, currentUser, loadUser, loginUrl, sessionExpired } from './auth'
const route = useRoute(); const error = ref('')
const publicPage = computed(() => ['/login', '/register'].includes(route.path))
const adminPage = computed(() => route.path.startsWith('/admin'))
const authorized = computed(() => authReady.value && !!currentUser.value && (!adminPage.value || currentUser.value.role === 'ADMIN'))
const openedEditor = ref('')
watch([authorized, () => route.path], () => { if (authorized.value && (/^\/(admin\/)?articles\/(new|\d+)$/.test(route.path) || route.path === '/profile')) openedEditor.value = route.path }, { immediate: true })
async function retry() { error.value = ''; try { await loadUser(true) } catch { error.value = '账户服务暂不可用，请重试。' } }
onMounted(retry)
</script>
<template>
  <Navbar active-page="account" />
  <ConfirmationDialog />
  <main class="account-page"><div class="account-shell">
    <nav class="account-nav" aria-label="用户中心"><RouterLink to="/articles">我的文章</RouterLink><RouterLink to="/profile">个人资料</RouterLink><RouterLink v-if="currentUser?.role === 'ADMIN'" to="/admin">管理员控制台</RouterLink></nav>
    <div v-if="sessionExpired" class="account-notice" role="alert">会话已过期。输入会保留，请<a :href="loginUrl()">重新登录</a>后重试。</div>
    <RouterView v-if="publicPage || authorized || (sessionExpired && openedEditor === route.path)" :key="route.path" />
    <section v-else class="account-card account-empty" :aria-busy="!authReady">
      <h1>{{ !authReady ? '正在确认登录状态…' : authIssue ? '账户服务暂不可用' : currentUser ? '没有管理员权限' : '请先登录' }}</h1><p>{{ error || authIssue || (currentUser ? '此控制台仅管理员可访问。' : '登录后可以保存草稿和提交文章审核。') }}</p><a v-if="authReady && !currentUser && !authIssue" class="account-button primary" :href="loginUrl()">前往登录</a><RouterLink v-else-if="currentUser" class="account-button" to="/articles">返回我的文章</RouterLink><button class="account-button" @click="retry">重新检查</button>
    </section>
  </div></main>
</template>
