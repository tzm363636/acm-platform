<script setup lang="ts">
import UiIcon from './UiIcon.vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { concealLeavingSurface } from '../composables/surfaceTransition'

withDefaults(defineProps<{
  activePage?: 'home' | 'oj' | 'sharing' | 'about' | 'milestone' | 'author'
  showSearch?: boolean
}>(), { activePage: 'home', showSearch: false })

const emit = defineEmits<{ search: []; dismissSearch: [] }>()
const navItems = [
  { label: '首页', href: './', page: 'home' },
  { label: '在线评测', href: './oj.html', page: 'oj' },
  { label: '代码分享', href: './code-sharing.html', page: 'sharing' },
  { label: '网站介绍', href: './about.html', page: 'about' },
  { label: '成长足迹', href: './milestone.html', page: 'milestone' },
] as const

const menuOpen = ref(false)
const entryOpen = ref(false)
const header = ref<HTMLElement | null>(null)
const entryTrigger = ref<HTMLButtonElement | null>(null)
const mobileTrigger = ref<HTMLButtonElement | null>(null)
const searchTrigger = ref<HTMLButtonElement | null>(null)
function toggleEntry() { entryOpen.value = !entryOpen.value; menuOpen.value = false; emit('dismissSearch') }
function toggleMobile() { menuOpen.value = !menuOpen.value; entryOpen.value = false; emit('dismissSearch') }
function focusSearchTrigger() { searchTrigger.value?.focus({ preventScroll: true }) }
defineExpose({ focusSearchTrigger })
function escape(event: KeyboardEvent) {
  if (event.key !== 'Escape' || (!entryOpen.value && !menuOpen.value)) return
  const trigger = entryOpen.value ? entryTrigger.value : mobileTrigger.value
  closeMenus(); trigger?.focus({ preventScroll: true }); event.preventDefault()
}
function outside(event: PointerEvent) { if (event.target instanceof Node && !header.value?.contains(event.target)) closeMenus() }
onMounted(() => { document.addEventListener('keydown', escape); document.addEventListener('pointerdown', outside) })
onBeforeUnmount(() => { document.removeEventListener('keydown', escape); document.removeEventListener('pointerdown', outside) })

function closeMenus() {
  menuOpen.value = false
  entryOpen.value = false
}
</script>

<template>
  <header ref="header" class="site-header">
    <div class="container nav-inner">
      <a class="brand" href="./" aria-label="ACM Code Share，返回首页" @click="closeMenus">
        <span class="brand-mark" aria-hidden="true">&lt;/&gt;</span>
        <span>ACM Code Share</span>
      </a>

      <nav class="desktop-nav" aria-label="主导航">
        <a v-for="item in navItems" :key="item.page" :class="{ active: activePage === item.page }" :href="item.href" :aria-current="activePage === item.page ? 'page' : undefined">{{ item.label }}</a>
      </nav>

      <div class="nav-actions">
        <button v-if="showSearch" ref="searchTrigger" class="nav-search-button" type="button" aria-label="搜索文章" @click="closeMenus(); emit('search')">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.8" cy="10.8" r="7.2"/><path d="m16.2 16.2 5 5"/></svg>
        </button>
        <div class="entry-wrap">
          <button ref="entryTrigger" class="entry-button" type="button" :aria-expanded="entryOpen" aria-controls="entry-menu" @click="toggleEntry">
            进入平台 <span aria-hidden="true"><UiIcon name="chevron" /></span>
          </button>
          <Transition name="surface" @before-leave="concealLeavingSurface"><div v-if="entryOpen" id="entry-menu" class="entry-menu" :inert="!entryOpen" :aria-hidden="!entryOpen">
            <a href="./oj.html" @click="closeMenus"><strong>在线评测</strong><small>进入 OJ 页面</small></a>
            <a href="./code-sharing.html" @click="closeMenus"><strong>代码分享</strong><small>进入代码交流分享页面</small></a>
          </div></Transition>
        </div>
        <button ref="mobileTrigger" class="mobile-toggle" type="button" :aria-expanded="menuOpen" aria-controls="mobile-menu" aria-label="切换导航菜单" @click="toggleMobile">
          <span></span><span></span><span></span>
        </button>
      </div>
    </div>
    <Transition name="surface" @before-leave="concealLeavingSurface"><nav v-if="menuOpen" id="mobile-menu" class="mobile-nav" aria-label="移动端导航" :inert="!menuOpen" :aria-hidden="!menuOpen">
      <a v-for="item in navItems" :key="item.page" :href="item.href" :aria-current="activePage === item.page ? 'page' : undefined" @click="closeMenus">{{ item.label }}</a>
    </nav></Transition>
  </header>
</template>
