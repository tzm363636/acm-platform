<script setup lang="ts">
import { ref } from 'vue'

withDefaults(defineProps<{
  activePage?: 'home' | 'oj' | 'sharing' | 'about' | 'milestone'
  showSearch?: boolean
}>(), { activePage: 'home', showSearch: false })

const emit = defineEmits<{ search: [] }>()
const navItems = [
  { label: '首页', href: './', page: 'home' },
  { label: '在线评测', href: './oj.html', page: 'oj' },
  { label: '代码分享', href: './code-sharing.html', page: 'sharing' },
  { label: '网站介绍', href: './about.html', page: 'about' },
  { label: '成长足迹', href: './milestone.html', page: 'milestone' },
] as const

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
      <a class="brand" href="./" aria-label="ACM Code Share，返回首页" @click="closeMenus">
        <span class="brand-mark" aria-hidden="true">&lt;/&gt;</span>
        <span>ACM Code Share</span>
      </a>

      <nav class="desktop-nav" aria-label="主导航">
        <a v-for="item in navItems" :key="item.page" :class="{ active: activePage === item.page }" :href="item.href" :aria-current="activePage === item.page ? 'page' : undefined">{{ item.label }}</a>
      </nav>

      <div class="nav-actions">
        <button v-if="showSearch" class="nav-search-button" type="button" aria-label="搜索文章" @click="emit('search')">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.8" cy="10.8" r="7.2"/><path d="m16.2 16.2 5 5"/></svg>
        </button>
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
      <a v-for="item in navItems" :key="item.page" :href="item.href" :aria-current="activePage === item.page ? 'page' : undefined" @click="closeMenus">{{ item.label }}</a>
    </nav>
  </header>
</template>
