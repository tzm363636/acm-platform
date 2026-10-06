<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getData, type ServerPage } from '../api/database'
import { statusNames, time, type ArticleSummary, type ArticleStatus } from './types'
import { currentUser } from './auth'
const props = withDefaults(defineProps<{ admin?: boolean }>(), { admin: false }), route = useRoute(), router = useRouter()
const q = ref(String(route.query.q || '').slice(0, 100)), status = ref<ArticleStatus | ''>(Object.hasOwn(statusNames, String(route.query.status)) ? route.query.status as ArticleStatus : '')
const page = ref(Math.max(1, Number(route.query.page) || 1)), size = ref([10,20,50].includes(Number(route.query.size)) ? Number(route.query.size) : 10)
const sort = ref(route.query.sort === 'submitted' ? 'submitted' : 'updated'), direction = ref(route.query.direction === 'asc' ? 'asc' : 'desc')
const result = ref<ServerPage<ArticleSummary>>(), loading = ref(false), error = ref(''), statisticsError = ref('')
const statistics = ref<{ users: number; articles: { status: ArticleStatus; count: number }[] }>(); let version = 0, restoringLocation = false
const title = computed(() => props.admin ? '管理员控制台' : '我的文章')
const pending = computed(() => statistics.value?.articles.find(s => s.status === 'PENDING')?.count || 0)
const destination = (id: number) => ({ path: `${props.admin ? '/admin/articles' : '/articles'}/${id}`, query: { list: route.fullPath } })
async function load() {
  const mine = ++version; loading.value = true; error.value = ''
  const params = { q: q.value, status: status.value, page: page.value, size: size.value, sort: sort.value, direction: direction.value }
  await router.replace({ path: route.path, query: { ...params, page: String(page.value), size: String(size.value) } })
  if (props.admin) void getData<typeof statistics.value>('/admin/statistics').then(r => { if (mine === version) { statistics.value = r; statisticsError.value = '' } }).catch(e => { if (mine === version) statisticsError.value = e.message })
  try { const r = await getData<ServerPage<ArticleSummary>>(props.admin ? '/admin/articles' : '/account/articles', params); if (mine === version) { result.value = r; page.value = r.page; if (r.page !== params.page) await router.replace({ path: route.path, query: { ...params, page: String(r.page), size: String(size.value) } }) } }
  catch (e) { if (mine === version) error.value = (e as Error).message } finally { if (mine === version) loading.value = false }
}
function filter() { if (restoringLocation) return; page.value = 1; void load() }
function clear() { const changed = status.value !== ''; q.value = ''; status.value = ''; page.value = 1; if (!changed) filter() }
function pendingQueue() { const changed = status.value !== 'PENDING' || sort.value !== 'submitted' || direction.value !== 'asc'; status.value = 'PENDING'; sort.value = 'submitted'; direction.value = 'asc'; if (!changed) filter() }
watch([status, size, sort, direction], filter)
// Back/forward and a same-page navigation must keep the URL and visible results consistent.
watch(() => route.query, query => {
  const next = { q: String(query.q || '').slice(0, 100), status: Object.hasOwn(statusNames, String(query.status)) ? query.status as ArticleStatus : '', page: Math.max(1, Number(query.page) || 1), size: [10,20,50].includes(Number(query.size)) ? Number(query.size) : 10, sort: query.sort === 'submitted' ? 'submitted' : 'updated', direction: query.direction === 'asc' ? 'asc' : 'desc' }
  if (next.q === q.value && next.status === status.value && next.page === page.value && next.size === size.value && next.sort === sort.value && next.direction === direction.value) return
  restoringLocation = true
  q.value = next.q; status.value = next.status as ArticleStatus | ''; page.value = next.page; size.value = next.size; sort.value = next.sort; direction.value = next.direction
  void load(); void nextTick(() => { restoringLocation = false })
})
watch(() => currentUser.value?.displayName, (name, old) => { if (name && old && name !== old) void load() })
onMounted(load)
</script>
<template>
  <header class="account-heading"><div><span class="account-eyebrow">{{ admin ? 'ADMIN WORKSPACE' : 'YOUR WRITING' }}</span><h1>{{ title }}</h1><p>{{ admin ? '审核投稿、管理文章，让分享保持质量。' : '从一份草稿开始，把你的思路分享给大家。' }}</p></div><RouterLink class="account-button primary" :to="{ path: '/articles/new', query: { list: route.fullPath } }">新建文章</RouterLink></header>
  <nav v-if="admin" class="account-tabs" aria-label="管理员功能"><RouterLink to="/admin">文章与审核</RouterLink><RouterLink to="/admin/users">用户列表</RouterLink><RouterLink to="/admin/taxonomy">分类与标签</RouterLink></nav>
  <div v-if="statistics" class="account-statistics"><span>真实账户 <strong>{{ statistics.users }}</strong></span><button class="account-text-button" @click="pendingQueue">待审核 <strong>{{ pending }}</strong> · 查看队列</button><span v-for="item in statistics.articles.filter(s => s.status !== 'PENDING')" :key="item.status">{{ statusNames[item.status] }} <strong>{{ item.count }}</strong></span></div>
  <p v-if="statisticsError" class="account-feedback error" role="alert">统计加载失败。<button class="account-text-button" @click="load">重试</button></p>
  <section class="account-card"><form class="account-filters" @submit.prevent="filter"><label>搜索<input v-model="q" placeholder="文章标题或作者" maxlength="100" /></label><label>状态<select v-model="status"><option value="">全部状态</option><option v-for="(label, key) in statusNames" :key="key" :value="key">{{ label }}</option></select></label><label>排序依据<select v-model="sort"><option value="updated">更新时间</option><option value="submitted">投稿时间</option></select></label><label>排序方向<select v-model="direction"><option value="desc">↓ 最新在前</option><option value="asc">↑ 最早在前</option></select></label><button class="account-button" :disabled="loading">搜索</button><button type="button" class="account-text-button" @click="clear">清除筛选</button></form>
    <p class="account-feedback" :class="{ error }" role="status">{{ error || (loading ? '正在读取列表…' : '') }} <button v-if="error" class="account-text-button" @click="load">重试</button></p>
    <div v-if="!result && loading" class="account-empty" aria-busy="true">正在加载文章…</div>
    <div v-else-if="result?.total === 0 && !error" class="account-empty"><h2>{{ q || status ? '没有符合条件的文章' : '还没有文章' }}</h2><p>{{ q || status ? '调整条件或清除筛选后再试。' : '新建草稿，准备好后提交审核。' }}</p><button v-if="q || status" class="account-button" @click="clear">清除筛选</button><RouterLink v-else class="account-button primary" to="/articles/new">开始写作</RouterLink></div>
    <div v-else class="account-article-list" :aria-busy="loading"><article v-for="article in result?.items" :key="article.id"><div><span class="account-badge" :data-status="article.status">{{ statusNames[article.status] }}</span><span v-if="article.featured" class="account-muted"> · 推荐文章</span><h2><RouterLink :to="destination(article.id)">{{ article.title }}</RouterLink></h2><p>{{ article.summary }}</p><small>{{ article.author }} · {{ article.category }} · {{ sort === 'submitted' ? `投稿于 ${time(article.submittedAt)}` : `更新于 ${time(article.updatedAt)}` }}</small><p v-if="article.status === 'REJECTED'" class="account-rejection">驳回原因：{{ article.rejectionReason || '未填写' }}</p></div><button class="account-button" @click="router.push(destination(article.id))">{{ admin && article.status === 'PENDING' ? '预览与审核' : '查看文章' }}</button></article></div>
    <footer v-if="result" class="account-pagination"><span>共 {{ result.total }} 篇 · {{ result.start }}–{{ result.end }}</span><div><button class="account-button" :disabled="loading || page <= 1" @click="page--; load()">上一页</button><span>{{ page }} / {{ result.pages }}</span><button class="account-button" :disabled="loading || page >= result.pages" @click="page++; load()">下一页</button></div><label>每页<select v-model="size"><option :value="10">10 篇</option><option :value="20">20 篇</option><option :value="50">50 篇</option></select></label></footer>
  </section>
</template>

