<script setup lang="ts">
import UiIcon from './UiIcon.vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { concealLeavingSurface } from '../composables/surfaceTransition'
import { authStatus, authBusy, currentUser, loadUser, loginUrl, logout } from '../account/auth'
import { requestLeave } from '../account/leaveGuard'

withDefaults(defineProps<{
  activePage?: 'home' | 'oj' | 'sharing' | 'about' | 'milestone' | 'author' | 'account'
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
const authError = ref(''), signingOut = ref(false)
async function signOut() { if (signingOut.value || authBusy.value || !await requestLeave()) return; signingOut.value = true; authError.value = ''; try { await logout(); closeMenus() } catch (e) { authError.value = (e as Error).message } finally { signingOut.value = false } }
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
onMounted(() => { void loadUser(); document.addEventListener('keydown', escape); document.addEventListener('pointerdown', outside) })
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
        <a v-if="authStatus === 'anonymous' || authStatus === 'expired'" class="nav-account-link" :href="loginUrl()">登录 / 注册</a>
        <button v-if="showSearch" ref="searchTrigger" class="nav-search-button" type="button" aria-label="搜索文章" @click="closeMenus(); emit('search')">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.8" cy="10.8" r="7.2"/><path d="m16.2 16.2 5 5"/></svg>
        </button>
        <div class="entry-wrap">
          <button ref="entryTrigger" class="entry-button" type="button" :aria-expanded="entryOpen" aria-controls="entry-menu" @click="toggleEntry">
            <span class="nav-entry-label" :aria-busy="authStatus === 'checking'">{{ authStatus === 'checking' ? '确认身份…' : authStatus === 'unavailable' ? '账户连接异常' : currentUser?.displayName || '进入平台' }}</span> <span aria-hidden="true"><UiIcon name="chevron" /></span>
          </button>
          <Transition name="surface" @before-leave="concealLeavingSurface"><div v-if="entryOpen" id="entry-menu" class="entry-menu" :inert="!entryOpen" :aria-hidden="!entryOpen">
            <a href="./oj.html" @click="closeMenus"><strong>在线评测</strong><small>进入 OJ 页面</small></a>
            <a href="./code-sharing.html" @click="closeMenus"><strong>代码分享</strong><small>进入代码交流分享页面</small></a>
            <template v-if="authStatus === 'authenticated' && currentUser"><a href="./account.html#/articles" @click="closeMenus"><strong>我的文章</strong><small>草稿、投稿与审核状态</small></a><a href="./account.html#/profile" @click="closeMenus"><strong>个人资料</strong><small>{{ currentUser.username }}</small></a><a v-if="currentUser.role === 'ADMIN'" href="./account.html#/admin" @click="closeMenus"><strong>管理员控制台</strong><small>审核、文章与用户管理</small></a><button type="button" :disabled="signingOut || authBusy" @click="signOut">{{ signingOut ? '正在退出…' : '退出登录' }}</button></template>
            <template v-else-if="authStatus === 'anonymous' || authStatus === 'expired'"><a :href="loginUrl()"><strong>登录</strong><small>进入用户中心</small></a><a :href="loginUrl().replace('#/login', '#/register')"><strong>注册</strong><small>创建普通用户账户</small></a></template><p v-else class="nav-auth-status" role="status">{{ authStatus === 'checking' ? '正在确认登录状态…' : '暂时无法确认登录状态，已有输入会保留。' }}<button v-if="authStatus === 'unavailable'" type="button" @click="loadUser(true)">重新检查</button></p><p v-if="authError" class="nav-auth-error" role="alert">{{ authError }}</p>
          </div></Transition>
        </div>
        <button ref="mobileTrigger" class="mobile-toggle" type="button" :aria-expanded="menuOpen" aria-controls="mobile-menu" aria-label="切换导航菜单" @click="toggleMobile">
          <span></span><span></span><span></span>
        </button>
      </div>
    </div>
    <Transition name="surface" @before-leave="concealLeavingSurface"><nav v-if="menuOpen" id="mobile-menu" class="mobile-nav" aria-label="移动端导航" :inert="!menuOpen" :aria-hidden="!menuOpen">
      <a v-for="item in navItems" :key="item.page" :href="item.href" :aria-current="activePage === item.page ? 'page' : undefined" @click="closeMenus">{{ item.label }}</a>
      <template v-if="authStatus === 'anonymous' || authStatus === 'expired'"><a :href="loginUrl()">登录</a><a :href="loginUrl().replace('#/login', '#/register')">注册</a></template><template v-else-if="authStatus === 'authenticated' && currentUser"><p class="mobile-account-name">已登录：{{ currentUser.displayName }}</p><a href="./account.html#/articles">我的文章</a><a href="./account.html#/profile">个人资料</a><a v-if="currentUser.role === 'ADMIN'" href="./account.html#/admin">管理员控制台</a><button type="button" :disabled="signingOut || authBusy" @click="signOut">{{ signingOut ? '正在退出…' : '退出登录' }}</button><p v-if="authError" class="nav-auth-error" role="alert">{{ authError }}</p></template><p v-else class="mobile-account-name" role="status">{{ authStatus === 'checking' ? '正在确认身份…' : '账户服务暂不可用' }}<button v-if="authStatus === 'unavailable'" type="button" @click="loadUser(true)">重新检查</button></p>
    </nav></Transition>
  </header>
</template>
<style scoped>
.nav-account-link{font-size:14px;color:#285a83;white-space:nowrap}.nav-entry-label{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:100px;transform:none!important;margin-left:0!important;vertical-align:middle}.entry-menu button{width:100%;padding:12px 16px;text-align:left;background:none;border:0;color:#9e3348;cursor:pointer}.entry-menu button:disabled{opacity:.6}.nav-auth-error{padding:8px 16px;color:#9e3348;font-size:13px}.entry-menu{max-height:calc(100dvh - 100px);overflow-y:auto}@media(max-width:1050px){.nav-account-link{display:none}}@media(max-width:450px){.nav-entry-label{max-width:64px}}
@media(max-width:360px){.nav-entry-label{max-width:44px}}
.mobile-nav{max-height:calc(100dvh - 80px);overflow-y:auto}.mobile-account-name{grid-column:1/-1;margin:0;padding:10px 16px;font-size:13px;overflow-wrap:anywhere;color:#58718e}.mobile-nav button{padding:12px 16px;text-align:left;font-size:14px;background:none;border:0;color:#9e3348;cursor:pointer}.mobile-nav button:disabled{opacity:.6;cursor:not-allowed}
.nav-entry-label{display:inline-block;width:100px}.nav-auth-status{padding:12px 16px;font-size:13px;line-height:1.7;color:#58718e}@media(max-width:450px){.nav-entry-label{width:64px}}@media(max-width:360px){.nav-entry-label{width:44px}}
</style>
