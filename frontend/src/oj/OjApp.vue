<script setup lang="ts">
import Navbar from '../components/Navbar.vue'
import { demoIdentity, disableDemoIdentity, enableDemoIdentity, listReturnUrl, storageNotice } from './store'
</script>
<template>
  <Navbar active-page="oj" />
  <main class="oj-page">
    <div class="oj-shell">
      <nav class="oj-nav" aria-label="在线评测模块">
        <RouterLink :to="listReturnUrl()" :class="{ selected: $route.path.startsWith('/problem') }">▤ 题库</RouterLink>
        <RouterLink to="/submissions" :class="{ selected: $route.path.startsWith('/submission') }">▣ 提交记录</RouterLink>
        <button disabled title="登录与账户系统尚未接入">♙ 个人中心（预留）</button>
      </nav>
      <div class="oj-demo-banner" role="note"><span>ⓘ</span><div><strong>演示模式</strong> · 登录功能与真实判题未接入。运行／提交仅展示固定模拟流程，不执行或判断用户代码。<small>演示记录仅保存在本机，与真实评测分开。{{ demoIdentity ? '当前身份：本机演示用户（非真实账户）。' : '个人状态需要登录；可启用本机演示身份体验。' }}</small></div><button class="oj-button small" @click="demoIdentity ? disableDemoIdentity() : enableDemoIdentity()">{{ demoIdentity ? '切换为未登录视图' : '启用演示身份' }}</button></div>
      <p v-if="storageNotice" class="oj-warning" role="status">{{ storageNotice }}</p>
      <RouterView :key="$route.path" />
    </div>
  </main>
</template>
