<script setup lang="ts">
import { ref } from 'vue'

const menuOpen = ref(false)
const entryOpen = ref(false)

function closeMenus() {
  menuOpen.value = false
  entryOpen.value = false
}
</script>

<template>
  <header class="site-header">
    <div class="container nav-inner">
      <a class="brand" href="#home" aria-label="ACM Code Share，返回首页" @click="closeMenus">
        <span class="brand-mark" aria-hidden="true">&lt;/&gt;</span>
        <span>ACM Code Share</span>
      </a>

      <nav class="desktop-nav" aria-label="主导航">
        <a class="active" href="#home">首页</a>
        <a href="#platform">平台入口</a>
        <a href="#sharing">内容分享</a>
        <a href="#about">网站介绍</a>
        <a href="#milestone">成长足迹</a>
      </nav>

      <div class="nav-actions">
        <div class="entry-wrap">
          <button class="entry-button" type="button" :aria-expanded="entryOpen" aria-controls="entry-menu" @click="entryOpen = !entryOpen">
            进入平台 <span aria-hidden="true">⌄</span>
          </button>
          <div v-if="entryOpen" id="entry-menu" class="entry-menu">
            <a href="./oj.html" @click="closeMenus"><strong>在线评测</strong><small>进入 OJ 页面</small></a>
            <a href="./code-sharing.html" @click="closeMenus"><strong>代码分享</strong><small>进入代码交流分享页面</small></a>
          </div>
        </div>
        <button class="mobile-toggle" type="button" :aria-expanded="menuOpen" aria-controls="mobile-menu" aria-label="切换导航菜单" @click="menuOpen = !menuOpen">
          <span></span><span></span><span></span>
        </button>
      </div>
    </div>
    <nav v-if="menuOpen" id="mobile-menu" class="mobile-nav" aria-label="移动端导航">
      <a href="#home" @click="closeMenus">首页</a>
      <a href="#platform" @click="closeMenus">平台入口</a>
      <a href="#sharing" @click="closeMenus">内容分享</a>
      <a href="#about" @click="closeMenus">网站介绍</a>
      <a href="#milestone" @click="closeMenus">成长足迹</a>
    </nav>
  </header>
</template>
