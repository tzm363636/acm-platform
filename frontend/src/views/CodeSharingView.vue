<script setup lang="ts">
import { computed, ref } from 'vue'

type Article = {
  id: number
  title: string
  summary: string
  category: string
  tags: string[]
  date: string
  views: string
  likes: number
  comments: number
  code?: string[]
  featured?: boolean
  wide?: boolean
}

const categories = ['全部', '题解', '算法模板', '竞赛经验', '408笔记', 'C++']
const hotTags = ['图论', 'DP', '数据结构', '数学', '搜索', '贪心', 'C++', '模板', '字符串', '动态规划', '最短路', '并查集', '树状数组']
const columns = [
  { name: '算法模板库', description: '常用算法模板整理，开箱即用', category: '算法模板', icon: 'book', color: 'orange' },
  { name: '408 知识整理', description: '数据结构、计算机组成原理等', category: '408笔记', icon: 'file', color: 'blue' },
  { name: '竞赛经验总结', description: '比赛复盘、心态调整与成长记录', category: '竞赛经验', icon: 'trophy', color: 'purple' },
]

const articles: Article[] = [
  {
    id: 1,
    title: '最短路算法：Dijkstra 与堆优化详解',
    summary: '从朴素 Dijkstra 到堆优化，详细讲解算法思想、复杂度分析与代码实现，附多道例题与模板，适合刷题与竞赛使用。',
    category: '题解', tags: ['图论', '最短路', 'C++', '题解'], date: '2024-03-12', views: '12.4k', likes: 392, comments: 56, featured: true,
    code: ['using P = pair<int,int>;', 'priority_queue<P,', '  vector<P>, greater<P>> q;', 'vector<int> dist(n, INF);', 'dist[s] = 0;', 'q.push({0, s});'],
  },
  {
    id: 2,
    title: '并查集（Disjoint Set Union）模板详解',
    summary: '从基础实现到按秩合并、路径压缩，附典型例题与易错点分析，适用于多种图论与连通性问题。',
    category: '算法模板', tags: ['数据结构', '并查集', '模板'], date: '2024-03-08', views: '8.2k', likes: 276, comments: 28,
    code: ['int find(int x) {', '  return fa[x] == x ? x :', '    fa[x] = find(fa[x]);', '}'],
  },
  {
    id: 3,
    title: '背包问题全解析：01 / 完全 / 多重背包',
    summary: '系统讲解背包问题的多种模型、状态转移方程与代码实现，例题讲解与思维拓展。',
    category: '题解', tags: ['动态规划', '背包问题', '题解', 'DP'], date: '2024-03-05', views: '8.7k', likes: 198, comments: 16,
    code: ['for (int i = 0; i < n; i++) {', '  for (int j = v; j >= w[i]; j--)', '    dp[j] = max(dp[j],', '      dp[j - w[i]] + val[i]);', '}'],
  },
  {
    id: 4,
    title: '树状数组（Fenwick）详解与模板',
    summary: '从原理到实现，详细讲解单点修改、区间查询等典型操作，附模板与经典题目。',
    category: '算法模板', tags: ['数据结构', '树状数组', '模板', 'C++'], date: '2024-02-28', views: '5.1k', likes: 143, comments: 12,
    code: ['int lowbit(int x) {', '  return x & -x;', '}', 'void add(int x, int v) {', '  for (; x <= n; x += lowbit(x))', '    tr[x] += v;', '}'],
  },
  {
    id: 5,
    title: 'KMP 字符串匹配模板与例题',
    summary: '详细讲解 KMP 算法的原理、next 数组求法与代码实现，附多道经典例题。',
    category: '算法模板', tags: ['字符串', 'KMP', '模板'], date: '2024-02-20', views: '4.6k', likes: 128, comments: 10,
    code: ['vector<int> nxt(m);', 'for (int i = 1, j = 0; i < m; i++) {', '  while (j && s[i] != s[j])', '    j = nxt[j - 1];', '  if (s[i] == s[j]) j++;', '  nxt[i] = j;', '}'],
  },
  {
    id: 6,
    title: 'ACM 竞赛复盘：从区域赛到省赛的成长之路',
    summary: '记录一次 ACM 竞赛的完整备赛与参赛过程，包含心态调整、团队协作、题目分析与经验总结。',
    category: '竞赛经验', tags: ['竞赛经验', 'ACM', '比赛总结', '成长'], date: '2024-02-20', views: '4.8k', likes: 167, comments: 24, wide: true,
  },
  {
    id: 7,
    title: '二分查找的边界：从模板到应用',
    summary: '梳理左右边界的写法与常见陷阱，用几个简单例子理解二分答案。',
    category: '算法模板', tags: ['搜索', '模板', 'C++'], date: '2024-02-15', views: '3.9k', likes: 95, comments: 8,
    code: ['while (l < r) {', '  int mid = (l + r) >> 1;', '  if (check(mid)) r = mid;', '  else l = mid + 1;', '}'],
  },
  {
    id: 8,
    title: '408 笔记：数据结构核心知识点',
    summary: '按照知识脉络整理线性表、树、图与排序，方便复习时快速回顾。',
    category: '408笔记', tags: ['408笔记', '数据结构', '复习'], date: '2024-02-10', views: '3.2k', likes: 81, comments: 6,
    code: ['线性表 · 栈与队列', '树与二叉树 · 图', '查找 · 排序', '时间复杂度 · 空间复杂度'],
  },
]

const activeCategory = ref('全部')
const searchInput = ref<HTMLInputElement | null>(null)
const searchDraft = ref('')
const searchTerm = ref('')
const showMore = ref(false)
const showAllTags = ref(false)
const showAuthor = ref(false)
const entryOpen = ref(false)
const mobileOpen = ref(false)
const mountainUrl = `${import.meta.env.BASE_URL}mountain-journey.svg`

const isFiltering = computed(() => activeCategory.value !== '全部' || searchTerm.value !== '')
const filteredArticles = computed(() => articles.filter((article) => {
  const categoryMatches = activeCategory.value === '全部' || article.category === activeCategory.value || article.tags.includes(activeCategory.value)
  const search = searchTerm.value.toLocaleLowerCase()
  const textMatches = !search || [article.title, article.summary, article.category, ...article.tags].join(' ').toLocaleLowerCase().includes(search)
  return categoryMatches && textMatches
}))
const featuredArticle = computed(() => filteredArticles.value.find((article) => article.featured))
const regularArticles = computed(() => filteredArticles.value.filter((article) => !article.featured && !article.wide))
const visibleArticles = computed(() => isFiltering.value || showMore.value ? regularArticles.value : regularArticles.value.slice(0, 4))
const wideArticle = computed(() => filteredArticles.value.find((article) => article.wide))
const visibleTags = computed(() => showAllTags.value ? hotTags : hotTags.slice(0, 10))

function scrollToResults() {
  document.getElementById('recommendations')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function selectCategory(category: string) {
  activeCategory.value = category
  searchTerm.value = ''
  searchDraft.value = ''
  showMore.value = false
  scrollToResults()
}

function submitSearch() {
  searchTerm.value = searchDraft.value.trim()
  activeCategory.value = '全部'
  scrollToResults()
}

function focusSearch() {
  searchInput.value?.focus()
  document.getElementById('search')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
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
    <header class="cs-header">
      <div class="cs-header-inner">
        <a class="cs-brand" href="./" aria-label="ACM Code Share，返回首页"><span class="cs-brand-mark" aria-hidden="true">&lt;/&gt;</span><strong>ACM Code Share</strong></a>
        <nav class="cs-nav" aria-label="主导航">
          <a href="./">首页</a>
          <a href="./#platform">平台入口</a>
          <a class="active" href="./code-sharing.html" aria-current="page">代码分享</a>
          <a href="./#about">网站介绍</a>
          <a href="./#milestone">成长足迹</a>
        </nav>
        <div class="cs-header-actions">
          <button class="cs-icon-button" type="button" aria-label="搜索文章" @click="focusSearch"><svg class="cs-icon"><use href="#icon-search"/></svg></button>
          <div class="cs-entry-wrap">
            <button class="cs-entry-button" type="button" :aria-expanded="entryOpen" aria-controls="cs-entry-menu" @click="entryOpen = !entryOpen">进入平台 <span aria-hidden="true">⌄</span></button>
            <div v-if="entryOpen" id="cs-entry-menu" class="cs-entry-menu">
              <a href="./oj.html">在线评测</a>
              <a href="./code-sharing.html">代码分享</a>
            </div>
          </div>
          <button class="cs-mobile-button" type="button" :aria-expanded="mobileOpen" aria-controls="cs-mobile-nav" aria-label="打开导航菜单" @click="mobileOpen = !mobileOpen">☰</button>
        </div>
      </div>
      <nav v-if="mobileOpen" id="cs-mobile-nav" class="cs-mobile-nav" aria-label="移动端导航">
        <a href="./">首页</a><a href="./#platform">平台入口</a><a href="./code-sharing.html" aria-current="page">代码分享</a><a href="./#about">网站介绍</a><a href="./#milestone">成长足迹</a>
      </nav>
    </header>

    <section class="cs-hero" aria-labelledby="page-title">
      <div class="cs-hero-inner">
        <div class="cs-hero-copy">
          <h1 id="page-title">代码分享<span class="cs-title-stroke" aria-hidden="true"></span></h1>
          <p>算法题解、模板沉淀、竞赛经验与学习笔记</p>
          <form id="search" class="cs-search" role="search" @submit.prevent="submitSearch">
            <svg class="cs-icon" aria-hidden="true"><use href="#icon-search"/></svg>
            <input ref="searchInput" v-model="searchDraft" type="search" aria-label="搜索题解、模板或标签" placeholder="搜索题解 / 模板 / 标签" />
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
          <div class="cs-section-title"><span aria-hidden="true">🔥</span><h2>{{ isFiltering ? '筛选结果' : '精选推荐' }}</h2><p>{{ isFiltering ? `找到 ${filteredArticles.length} 篇相关内容` : '优质的算法题解、模板与经验分享' }}</p></div>
          <button v-if="!isFiltering && regularArticles.length > 4" type="button" class="cs-more-link" @click="showMore = !showMore">{{ showMore ? '收起' : '查看更多' }} <svg class="cs-icon"><use href="#icon-arrow"/></svg></button>
        </div>

        <div v-if="filteredArticles.length === 0" class="cs-empty"><strong>暂时没有找到相关内容</strong><p>试试其他关键词或分类。</p><button type="button" @click="selectCategory('全部')">查看全部文章</button></div>

        <article v-if="featuredArticle" class="cs-featured-card">
          <div class="cs-code-preview cs-code-preview-dark">
            <span class="cs-hot-badge">🔥 热门</span>
            <div class="cs-window-dots" aria-hidden="true"><i></i><i></i><i></i></div>
            <div class="cs-code-lines"><div v-for="(line, index) in featuredArticle.code" :key="index"><span>{{ index + 1 }}</span><code>{{ line }}</code></div></div>
          </div>
          <div class="cs-featured-body">
            <h3>{{ featuredArticle.title }}</h3>
            <p>{{ featuredArticle.summary }}</p>
            <div class="cs-article-tags"><span v-for="tag in featuredArticle.tags" :key="tag">{{ tag }}</span></div>
            <div class="cs-article-meta"><span><svg class="cs-icon"><use href="#icon-user"/></svg> ACMer</span><span><svg class="cs-icon"><use href="#icon-calendar"/></svg> {{ featuredArticle.date }}</span><span><svg class="cs-icon"><use href="#icon-eye"/></svg> {{ featuredArticle.views }}</span><span><svg class="cs-icon"><use href="#icon-like"/></svg> {{ featuredArticle.likes }}</span><span><svg class="cs-icon"><use href="#icon-comment"/></svg> {{ featuredArticle.comments }}</span></div>
          </div>
        </article>

        <div v-if="visibleArticles.length" class="cs-article-grid">
          <article v-for="article in visibleArticles" :key="article.id" class="cs-article-card">
            <div class="cs-code-preview cs-code-preview-light">
              <div class="cs-window-dots" aria-hidden="true"><i></i><i></i><i></i></div>
              <div class="cs-code-lines"><div v-for="(line, index) in article.code" :key="index"><span>{{ index + 1 }}</span><code>{{ line }}</code></div></div>
            </div>
            <div class="cs-article-body">
              <h3>{{ article.title }}</h3>
              <p>{{ article.summary }}</p>
              <div class="cs-article-tags"><span v-for="tag in article.tags.slice(0, 4)" :key="tag">{{ tag }}</span></div>
              <div class="cs-article-meta"><span><svg class="cs-icon"><use href="#icon-user"/></svg> ACMer</span><span><svg class="cs-icon"><use href="#icon-calendar"/></svg> {{ article.date }}</span><span><svg class="cs-icon"><use href="#icon-eye"/></svg> {{ article.views }}</span><span><svg class="cs-icon"><use href="#icon-like"/></svg> {{ article.likes }}</span><span><svg class="cs-icon"><use href="#icon-comment"/></svg> {{ article.comments }}</span></div>
            </div>
          </article>
        </div>

        <article v-if="wideArticle" class="cs-wide-card">
          <img :src="mountainUrl" alt="登山者站在山峰上迎接日出" />
          <div class="cs-wide-body"><h3>{{ wideArticle.title }}</h3><p>{{ wideArticle.summary }}</p><div class="cs-article-tags"><span v-for="tag in wideArticle.tags" :key="tag">{{ tag }}</span></div><div class="cs-article-meta"><span><svg class="cs-icon"><use href="#icon-user"/></svg> ACMer</span><span><svg class="cs-icon"><use href="#icon-calendar"/></svg> {{ wideArticle.date }}</span><span><svg class="cs-icon"><use href="#icon-eye"/></svg> {{ wideArticle.views }}</span><span><svg class="cs-icon"><use href="#icon-like"/></svg> {{ wideArticle.likes }}</span><span><svg class="cs-icon"><use href="#icon-comment"/></svg> {{ wideArticle.comments }}</span></div></div>
        </article>
      </main>

      <aside class="cs-sidebar" aria-label="代码分享侧栏">
        <section class="cs-side-card cs-tags-card">
          <div class="cs-side-heading"><h2><span aria-hidden="true">🔥</span> 热门标签</h2><button type="button" @click="showAllTags = !showAllTags">{{ showAllTags ? '收起' : '查看更多' }} <svg class="cs-icon"><use href="#icon-arrow"/></svg></button></div>
          <div class="cs-hot-tags"><button v-for="(tag, index) in visibleTags" :key="tag" type="button" :class="[`tag-color-${index % 6}`, { active: activeCategory === tag }]" @click="selectCategory(tag)">{{ tag }}</button></div>
        </section>

        <section class="cs-side-card cs-columns-card">
          <div class="cs-side-heading"><h2><svg class="cs-icon"><use href="#icon-stack"/></svg> 推荐专栏</h2><button type="button" @click="selectCategory('全部')">查看更多 <svg class="cs-icon"><use href="#icon-arrow"/></svg></button></div>
          <div class="cs-column-list"><button v-for="column in columns" :key="column.name" type="button" @click="selectCategory(column.category)"><span class="cs-column-icon" :class="column.color"><svg class="cs-icon"><use :href="`#icon-${column.icon}`"/></svg></span><span class="cs-column-text"><strong>{{ column.name }}</strong><small>{{ column.description }}</small></span><span class="cs-column-arrow">›</span></button></div>
        </section>

        <section class="cs-side-card cs-author-card">
          <div class="cs-side-heading"><h2><span aria-hidden="true">♧</span> 关于作者</h2><button type="button" @click="showAuthor = !showAuthor">{{ showAuthor ? '收起' : '查看更多' }} <svg class="cs-icon"><use href="#icon-arrow"/></svg></button></div>
          <div class="cs-author-profile"><div class="cs-avatar" aria-hidden="true">&lt;/&gt;</div><div><h3>ACMer <span>Lv.5</span></h3><p>一个热爱算法与竞赛的开发者，分享题解、模板与成长经验。</p></div></div>
          <div class="cs-author-stats"><div><strong>102</strong><span>文章</span></div><div><strong>12.4k</strong><span>总阅读</span></div><div><strong>892</strong><span>获赞</span></div></div>
          <p v-if="showAuthor" class="cs-author-more">持续记录学习和竞赛路上的思考，欢迎一起交流。</p>
        </section>

        <div class="cs-side-banner" :style="{ backgroundImage: `url(${mountainUrl})` }"><strong>在代码中<br />遇见更好的自己</strong><span aria-hidden="true"></span></div>
      </aside>
    </div>
    <footer class="cs-footer">ACM Code Share · 让代码有答案，也让经验被看见</footer>
  </div>
</template>
