<script setup lang="ts">
import UiIcon from '../components/UiIcon.vue'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Navbar from '../components/Navbar.vue'
import { concealLeavingSurface } from '../composables/surfaceTransition'

import { articles as offlineArticles, articleUrl, type Article } from '../data/articles'
import { databaseMode, getData, type ServerPage } from '../api/database'

const categories = ['全部', '题解', '算法模板', '竞赛经验', '408笔记']
const hotTags = ['图论', '动态规划', '数据结构', '搜索', 'C++', '模板', '字符串', '最短路', '并查集', '树状数组', '竞赛经验', '408笔记']
const columns = [
  { name: '算法模板库', description: '常用算法模板整理，开箱即用', category: '算法模板', icon: 'book', color: 'orange' },
  { name: '408 知识整理', description: '数据结构、计算机组成原理等', category: '408笔记', icon: 'file', color: 'blue' },
  { name: '竞赛经验总结', description: '比赛复盘、心态调整与成长记录', category: '竞赛经验', icon: 'trophy', color: 'purple' },
]

const initialParams = new URLSearchParams(window.location.search)
const initialCategory = initialParams.get('category') || '全部'
const initialTag = initialParams.get('tag') === 'DP' ? '动态规划' : initialParams.get('tag') || ''
const activeCategory = ref(categories.includes(initialCategory) ? initialCategory : '全部')
const activeTag = ref(hotTags.includes(initialTag) ? initialTag : '')
const searchDraft = ref(initialParams.get('q') || '')
const searchTerm = ref(searchDraft.value.trim())
const showMore = ref(initialParams.get('more') === '1')
const showAllTags = ref(initialParams.get('tags') === 'all')
const searchOpen = ref(false)
const navbar = ref<InstanceType<typeof Navbar> | null>(null)
const headerSearchInput = ref<HTMLInputElement | null>(null)
const sidebar = ref<HTMLElement | null>(null)
const mountainUrl = `${import.meta.env.BASE_URL}mountain-journey.svg`
const avatarUrl = `${import.meta.env.BASE_URL}author-avatar.jpg`
let sidebarUpdate = 0
const articles = ref<Article[]>(databaseMode ? [] : offlineArticles)
const loading = ref(databaseMode);const loadError=ref('');const totalArticles=ref(offlineArticles.length)
const regularTotal=ref(0);const filteredTotal=ref(0)
let articleRequest=0;let preserveSidebar=false

const isFiltering = computed(() => activeCategory.value !== '全部' || activeTag.value !== '' || searchTerm.value !== '')
const filteredArticles = computed(() => databaseMode ? articles.value : articles.value.filter((article) => {
  const categoryMatches = activeCategory.value === '全部' || article.category === activeCategory.value
  const tagMatches = !activeTag.value || article.tags.includes(activeTag.value)
  const search = searchTerm.value.toLocaleLowerCase() === 'dp' ? '动态规划' : searchTerm.value.toLocaleLowerCase()
  const textMatches = !search || [article.title, article.summary, article.category, ...article.tags].join(' ').toLocaleLowerCase().includes(search)
  return categoryMatches && tagMatches && textMatches
}))
const featuredArticle = computed(() => filteredArticles.value.find((article) => article.featured))
const regularArticles = computed(() => filteredArticles.value.filter((article) => !article.featured && !article.wide))
const visibleArticles = computed(() => isFiltering.value || showMore.value ? regularArticles.value : regularArticles.value.slice(0, 4))
const wideArticle = computed(() => filteredArticles.value.find((article) => article.wide))
const visibleTags = computed(() => showAllTags.value ? hotTags : hotTags.slice(0, 10))
const resultKey = computed(() => `${activeCategory.value}|${activeTag.value}|${searchTerm.value}|${showMore.value}`)
const sharingQuery = computed(() => {
  const params = new URLSearchParams()
  if (activeCategory.value !== '全部') params.set('category', activeCategory.value)
  if (activeTag.value) params.set('tag', activeTag.value)
  if (searchTerm.value) params.set('q', searchTerm.value)
  if (showMore.value) params.set('more', '1')
  if (showAllTags.value) params.set('tags', 'all')
  return params.toString()
})
const articleCount=computed(()=>databaseMode?filteredTotal.value:filteredArticles.value.length)
const regularCount=computed(()=>databaseMode?regularTotal.value:regularArticles.value.length)
async function loadArticles() {
 const version=++articleRequest;loading.value=true;loadError.value=''
 const q=searchTerm.value.toLocaleLowerCase()==='dp'?'动态规划':searchTerm.value
 const params={q,category:activeCategory.value==='全部'?'':activeCategory.value,tag:activeTag.value,page:1}
 try {
  const [featured,regular,wide]=await Promise.all(['featured','regular','wide'].map(layout=>getData<ServerPage<Article>>('/articles',{...params,layout,size:layout==='regular'&&!isFiltering.value&&!showMore.value?4:100})))
  if(version!==articleRequest)return
  const previousTop=preserveSidebar?sidebar.value?.getBoundingClientRect().top:undefined
  loading.value=false;articles.value=[...featured.items,...regular.items,...wide.items];regularTotal.value=regular.total;filteredTotal.value=featured.total+regular.total+wide.total
  await nextTick()
  if(version===articleRequest&&previousTop!==undefined&&sidebar.value){const change=sidebar.value.getBoundingClientRect().top-previousTop;if(Math.abs(change)>1)window.scrollBy({top:change,behavior:'instant'})}
  preserveSidebar=false
 }catch(e){if(version===articleRequest)loadError.value=(e as Error).message}
 finally{if(version===articleRequest)loading.value=false}
}
watch(sharingQuery,()=>{if(databaseMode)void loadArticles()})
onMounted(async()=>{if(databaseMode){void loadArticles();try{totalArticles.value=(await getData<ServerPage<Article>>('/articles',{size:1})).total}catch{ /* List error offers retry. */ }}})

watch(sharingQuery, (query) => {
  window.history.replaceState(window.history.state, '', `${window.location.pathname}${query ? `?${query}` : ''}`)
}, { immediate: true })

function saveListPosition() {
  try {
    sessionStorage.setItem('acm-sharing-position', JSON.stringify({ query: sharingQuery.value, top: window.scrollY }))
  } catch { /* Navigation still works when browser storage is unavailable. */ }
}

onMounted(async () => {
  window.addEventListener('pagehide', saveListPosition)
  await nextTick()
  try {
    const saved = JSON.parse(sessionStorage.getItem('acm-sharing-position') || 'null')
    if (saved?.query === sharingQuery.value && Number.isFinite(saved.top)) {
      window.scrollTo({ top: saved.top, behavior: 'instant' })
    }
  } catch { /* Invalid or unavailable storage should not block the page. */ }
})
function closeSearch() { searchOpen.value = false; navbar.value?.focusSearchTrigger() }
function escapeSearch(event: KeyboardEvent) { if (event.key === 'Escape' && searchOpen.value) { closeSearch(); event.preventDefault() } }
onMounted(() => document.addEventListener('keydown', escapeSearch))
onBeforeUnmount(() => { window.removeEventListener('pagehide', saveListPosition); document.removeEventListener('keydown', escapeSearch) })

function selectCategory(category: string) {
  activeCategory.value = category
  showMore.value = false
}

function selectTag(tag: string) {
  void keepSidebarPosition(() => {
    if (activeTag.value === tag) {
      showAllArticles()
    } else {
      activeTag.value = tag
      showMore.value = false
    }
  })
}

async function keepSidebarPosition(update: () => void) {
  if(databaseMode)preserveSidebar=true
  const version = ++sidebarUpdate
  const previousTop = sidebar.value?.getBoundingClientRect().top
  update()
  await nextTick()
  // On narrow screens the results sit above the sidebar; keep the clicked controls in place.
  if (version === sidebarUpdate && previousTop !== undefined && sidebar.value) {
    const change = sidebar.value.getBoundingClientRect().top - previousTop
    if (Math.abs(change) > 1) window.scrollBy({ top: change, behavior: 'instant' })
  }
}

function browseCategory(category: string) {
  void keepSidebarPosition(() => selectCategory(category))
}

function clearFilters() {
  activeCategory.value = '全部'
  activeTag.value = ''
  searchTerm.value = ''
  searchDraft.value = ''
  showMore.value = false
}

function showAllArticles() {
  clearFilters()
  showMore.value = true
}

function submitSearch() {
  searchTerm.value = searchDraft.value.trim()
  showMore.value = false
  if (searchOpen.value) closeSearch()
}

async function focusSearch() {
  if (searchOpen.value) { closeSearch(); return }
  searchOpen.value = true
  if (searchOpen.value) {
    await nextTick()
    headerSearchInput.value?.focus({ preventScroll: true })
  }
}
</script>

<template>
  <svg class="icon-sprite" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <symbol id="icon-search" viewBox="0 0 24 24"><circle cx="10.8" cy="10.8" r="7.2"/><path d="m16.2 16.2 5 5"/></symbol>
    <symbol id="icon-arrow" viewBox="0 0 24 24"><path d="M4 12h15m-6-6 6 6-6 6"/></symbol>
    <symbol id="icon-user" viewBox="0 0 24 24"><circle cx="12" cy="7" r="3.5"/><path d="M4.5 20c0-4.2 3-6.5 7.5-6.5s7.5 2.3 7.5 6.5"/></symbol>
    <symbol id="icon-calendar" viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4m10-4v4M3 10h18"/></symbol>
    <symbol id="icon-eye" viewBox="0 0 24 24"><path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12Z"/><circle cx="12" cy="12" r="2.8"/></symbol>
    <symbol id="icon-like" viewBox="0 0 24 24"><path d="M8 10v11H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h4Zm0 0 5-8c1.8 0 2.4 1.5 2 3l-1 4h5a3 3 0 0 1 2.9 3.8l-1.6 6A3 3 0 0 1 17.4 21H8"/></symbol>
    <symbol id="icon-comment" viewBox="0 0 24 24"><path d="M4 4h16a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H8l-5 3V6a2 2 0 0 1 1-2Z"/></symbol>
    <symbol id="icon-book" viewBox="0 0 24 24"><path d="M12 5c-3.4-2-6.5-2-9-1v16c3-1 6-.8 9 1 3-1.8 6-2 9-1V4c-3-.8-5.6-1-9 1Zm0 0v16"/></symbol>
    <symbol id="icon-file" viewBox="0 0 24 24"><path d="M6 2h8l5 5v15H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Zm8 0v6h5M8 13h8m-8 4h8"/></symbol>
    <symbol id="icon-trophy" viewBox="0 0 24 24"><path d="M7 3h10v8a5 5 0 0 1-10 0V3Zm0 2H3v3a4 4 0 0 0 4 4m10-7h4v3a4 4 0 0 1-4 4m-5 4v4m-4 0h8m-9 2h10"/></symbol>
    <symbol id="icon-stack" viewBox="0 0 24 24"><path d="m12 2 9 5-9 5-9-5 9-5Zm-9 9 9 5 9-5M3 16l9 5 9-5"/></symbol>
  </svg>

  <div class="cs-page">
    <Navbar ref="navbar" active-page="sharing" show-search @search="focusSearch" @dismiss-search="searchOpen = false" />
    <Transition name="surface" @before-leave="concealLeavingSurface"><form v-if="searchOpen" class="cs-search-popover" role="search" :inert="!searchOpen" :aria-hidden="!searchOpen" @submit.prevent="submitSearch">
      <label for="header-article-search">搜索文章</label>
      <div><input id="header-article-search" ref="headerSearchInput" v-model="searchDraft" type="search" placeholder="题解 / 模板 / 标签" /><button type="submit">搜索</button></div>
    </form></Transition>

    <section class="cs-hero" aria-labelledby="page-title">
      <div class="cs-hero-inner">
        <div class="cs-hero-copy">
          <h1 id="page-title">代码分享<span class="cs-title-stroke" aria-hidden="true"></span></h1>
          <p>算法题解、模板沉淀、竞赛经验与学习笔记</p>
          <form id="search" class="cs-search" role="search" @submit.prevent="submitSearch">
            <svg class="cs-icon" aria-hidden="true"><use href="#icon-search"/></svg>
            <input v-model="searchDraft" type="search" aria-label="搜索题解、模板或标签" placeholder="搜索题解 / 模板 / 标签" />
            <button type="submit" aria-label="提交搜索"><svg class="cs-icon" aria-hidden="true"><use href="#icon-arrow"/></svg></button>
          </form>
          <div class="cs-categories" aria-label="文章分类">
            <button v-for="category in categories" :key="category" type="button" :class="{ active: activeCategory === category }" :aria-pressed="activeCategory === category" @click="selectCategory(category)">{{ category }}</button>
          </div>
        </div>
        <div class="cs-hero-art" aria-hidden="true">
          <span class="cs-star star-one">✦</span><span class="cs-star star-two">✦</span><span class="cs-star star-three">✦</span>
          <div class="cs-code-stack"><div class="cs-code-square">&lt;/&gt;</div></div>
          <p>让代码有答案，<br />也让经验被看见。</p>
          <span class="cs-quote-stroke"></span>
        </div>
      </div>
    </section>

    <div class="cs-content-grid">
      <main id="recommendations" class="cs-results">
        <div class="cs-section-heading">
          <div class="cs-section-title"><UiIcon name="spark" /><h2>{{ isFiltering ? '筛选结果' : '精选推荐' }}</h2><p>{{ isFiltering ? `找到 ${articleCount} 篇相关内容` : '优质的算法题解、模板与经验分享' }}</p></div>
          <button v-if="!isFiltering && regularCount > 4" type="button" class="cs-more-link" @click="showMore = !showMore">{{ showMore ? '收起' : '查看全部文章' }} <svg class="cs-icon"><use href="#icon-arrow"/></svg></button>
        </div>

        <div :key="resultKey" class="cs-results-content content-reveal">
        <div v-if="isFiltering" class="cs-active-filters" aria-label="已选筛选条件">
          <span class="cs-result-count" role="status" aria-live="polite">{{ articleCount }} 篇文章</span>
          <button v-if="activeCategory !== '全部'" type="button" :aria-label="`移除分类：${activeCategory}`" @click="selectCategory('全部')">{{ activeCategory }} <span aria-hidden="true">×</span></button>
          <button v-if="activeTag" type="button" :aria-label="`移除标签：${activeTag}`" @click="activeTag = ''">{{ activeTag }} <span aria-hidden="true">×</span></button>
          <button v-if="searchTerm" type="button" :aria-label="`移除搜索：${searchTerm}`" @click="searchTerm = ''; searchDraft = ''">关键词：{{ searchTerm }} <span aria-hidden="true">×</span></button>
          <button type="button" class="cs-clear-filters" @click="clearFilters">清除筛选</button>
        </div>

        <p v-if="loading || loadError" class="cs-result-count" :role="loadError ? 'alert' : 'status'">{{ loadError || '正在查询文章…' }} <button v-if="loadError" type="button" @click="loadArticles">重试</button></p>
        <div v-if="!loading && !loadError && filteredArticles.length === 0" class="cs-empty"><strong>暂时没有找到相关内容</strong><p>试试其他关键词，或移除部分筛选条件。</p><button type="button" @click="showAllArticles">查看全部文章</button></div>

        <a v-if="featuredArticle" class="cs-featured-card" :href="articleUrl(featuredArticle.id, sharingQuery)" :aria-label="`阅读文章：${featuredArticle.title}`" @click="saveListPosition">
          <div class="cs-code-preview cs-code-preview-dark">
            <span class="cs-hot-badge">精选</span>
            <div class="cs-window-dots" aria-hidden="true"><i></i><i></i><i></i></div>
            <div class="cs-code-lines"><div v-for="(line, index) in featuredArticle.preview" :key="index"><span>{{ index + 1 }}</span><code>{{ line }}</code></div></div>
          </div>
          <div class="cs-featured-body">
            <h3>{{ featuredArticle.title }}</h3>
            <p>{{ featuredArticle.summary }}</p>
            <div class="cs-article-tags"><span v-for="tag in featuredArticle.tags" :key="tag">{{ tag }}</span></div>
            <div class="cs-article-meta"><span><svg class="cs-icon"><use href="#icon-user"/></svg> 田振民</span><span>阅读全文 →</span></div>
          </div>
        </a>

        <div v-if="visibleArticles.length" class="cs-article-grid">
          <a v-for="article in visibleArticles" :key="article.id" class="cs-article-card" :href="articleUrl(article.id, sharingQuery)" :aria-label="`阅读文章：${article.title}`" @click="saveListPosition">
            <div class="cs-code-preview cs-code-preview-light">
              <div class="cs-window-dots" aria-hidden="true"><i></i><i></i><i></i></div>
              <div class="cs-code-lines"><div v-for="(line, index) in article.preview" :key="index"><span>{{ index + 1 }}</span><code>{{ line }}</code></div></div>
            </div>
            <div class="cs-article-body">
              <h3>{{ article.title }}</h3>
              <p>{{ article.summary }}</p>
              <div class="cs-article-tags"><span v-for="tag in article.tags.slice(0, 4)" :key="tag">{{ tag }}</span></div>
              <div class="cs-article-meta"><span><svg class="cs-icon"><use href="#icon-user"/></svg> 田振民</span><span>阅读全文 →</span></div>
            </div>
          </a>
        </div>

        <a v-if="wideArticle" class="cs-wide-card" :href="articleUrl(wideArticle.id, sharingQuery)" :aria-label="`阅读文章：${wideArticle.title}`" @click="saveListPosition">
          <img :src="mountainUrl" alt="登山者站在山峰上迎接日出" />
          <div class="cs-wide-body"><h3>{{ wideArticle.title }}</h3><p>{{ wideArticle.summary }}</p><div class="cs-article-tags"><span v-for="tag in wideArticle.tags" :key="tag">{{ tag }}</span></div><div class="cs-article-meta"><span><svg class="cs-icon"><use href="#icon-user"/></svg> 田振民</span><span>阅读全文 →</span></div></div>
        </a>
        </div>
      </main>

      <aside ref="sidebar" class="cs-sidebar" aria-label="代码分享侧栏">
        <section class="cs-side-card cs-tags-card">
          <div class="cs-side-heading"><h2><UiIcon name="spark" /> 热门标签</h2><button type="button" @click="showAllTags = !showAllTags">{{ showAllTags ? '收起' : '查看更多' }} <svg class="cs-icon"><use href="#icon-arrow"/></svg></button></div>
          <div class="cs-hot-tags"><button v-for="(tag, index) in visibleTags" :key="tag" type="button" :class="[`tag-color-${index % 6}`, { active: activeTag === tag }]" :aria-pressed="activeTag === tag" @click="selectTag(tag)">{{ tag }}</button></div>
        </section>

        <section class="cs-side-card cs-columns-card">
          <div class="cs-side-heading"><h2><svg class="cs-icon"><use href="#icon-stack"/></svg> 分类精选</h2><button type="button" @click="keepSidebarPosition(showAllArticles)">全部文章 <svg class="cs-icon"><use href="#icon-arrow"/></svg></button></div>
          <div class="cs-column-list"><button v-for="column in columns" :key="column.name" type="button" @click="browseCategory(column.category)"><span class="cs-column-icon" :class="column.color"><svg class="cs-icon"><use :href="`#icon-${column.icon}`"/></svg></span><span class="cs-column-text"><strong>{{ column.name }}</strong><small>{{ column.description }}</small></span><span class="cs-column-arrow">›</span></button></div>
        </section>

        <section class="cs-side-card cs-author-card">
          <div class="cs-side-heading"><h2><UiIcon name="user" /> 关于作者</h2><a class="cs-side-link" href="./author.html">查看更多 <svg class="cs-icon"><use href="#icon-arrow"/></svg></a></div>
          <div class="cs-author-profile"><img class="cs-avatar" :src="avatarUrl" alt="田振民的头像" width="68" height="68" /><div><h3>田振民</h3><p>软件工程专业学生，热爱算法、编程与技术探索。</p></div></div>
          <div class="cs-author-stats"><div><strong>{{ totalArticles }}</strong><span>站内文章</span></div><div><strong>4</strong><span>内容分类</span></div></div>
        </section>

        <div class="cs-side-banner" :style="{ backgroundImage: `url(${mountainUrl})` }"><strong>在代码中<br />遇见更好的自己</strong><span aria-hidden="true"></span></div>
      </aside>
    </div>
    <footer class="cs-footer">ACM Code Share · 让代码有答案，也让经验被看见</footer>
  </div>
</template>
