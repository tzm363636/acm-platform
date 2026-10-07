<script setup lang="ts">
import UiIcon from '../components/UiIcon.vue'
import Navbar from '../components/Navbar.vue'
import { databaseMode } from '../api/database'
import { authStatus, currentUser } from '../account/auth'
import { demoIdentity, disableDemoIdentity, enableDemoIdentity, listReturnUrl, storageNotice } from './store'
</script>
<template>
  <Navbar active-page="oj" />
  <main class="oj-page">
    <div class="oj-shell">
      <nav class="oj-nav" aria-label="在线评测模块">
        <RouterLink :to="listReturnUrl()" :class="{ selected: $route.path.startsWith('/problem') }"><UiIcon name="book" /> 题库</RouterLink>
        <RouterLink to="/submissions" :class="{ selected: $route.path.startsWith('/submission') }"><UiIcon name="list" /> 提交记录</RouterLink>
        <a href="./account.html#/profile"><UiIcon name="user" /> 个人中心</a>
      </nav>
      <div class="oj-demo-banner" role="note"><span>ⓘ</span><div><strong>演示模式</strong> · 真实判题未接入。运行／提交仅展示固定模拟流程，不执行或判断用户代码。<small>{{ authStatus === 'checking' ? '正在确认真实账户身份。' : authStatus === 'unavailable' ? '真实账户服务暂不可用。' : authStatus === 'expired' ? '真实账户会话已过期。' : currentUser ? `已登录：${currentUser.displayName}。` : '当前未登录真实账户。' }}{{ databaseMode ? '演示记录保存在数据库，与真实评测分开；草稿仍保存在本机。' : '演示记录仅保存在本机，与真实评测分开。' }}{{ demoIdentity ? '当前评测身份：本机演示用户（非真实账户）。' : '可启用演示身份体验模拟练习状态。' }}</small></div><button class="oj-button small" @click="demoIdentity ? disableDemoIdentity() : enableDemoIdentity()">{{ demoIdentity ? '关闭演示身份' : '启用演示身份' }}</button></div>
      <p v-if="storageNotice" class="oj-warning" role="status">{{ storageNotice }}</p>
      <RouterView :key="$route.path" />
    </div>
  </main>
</template>
<style scoped>
@media (max-width: 650px) {
  .oj-demo-banner > div { flex: 1 1 calc(100% - 30px); min-width: 0; }
  .oj-demo-banner > button { margin-left: 27px; }
}
</style>
