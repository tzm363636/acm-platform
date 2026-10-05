<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ojApi, pageSlice, personalStatus, problemStats } from './api'
import { demoIdentity, records, rememberListUrl, listReturnUrl } from './store'
import type { Problem } from './types'
import Sidebar from './Sidebar.vue'
import Pagination from './Pagination.vue'

const route = useRoute(); const router = useRouter()
const remembered = new URLSearchParams(listReturnUrl().split('?')[1] || '')
const initial = (key: string, fallback = '') => String(route.query[key] ?? (Object.keys(route.query).length ? fallback : remembered.get(key) ?? fallback))
const search = ref(initial('q')); const difficulty = ref(initial('difficulty')); const tag = ref(initial('tag'))
const status = ref(demoIdentity.value ? initial('status') : ''); const hideTags = ref(initial('hideTags') === '1')
const page = ref(Math.max(1, Number(initial('page', '1')) || 1)); const size = ref([4, 8, 16].includes(Number(initial('size', '8'))) ? Number(initial('size', '8')) : 8)
const sort = ref(initial('sort', 'id')); const direction = ref(initial('direction', 'asc') === 'desc' ? 'desc' : 'asc')
const items = ref<Problem[]>([]); const loading = ref(true); const error = ref('')
const tags = computed(() => [...new Set(items.value.flatMap(p => p.tags))])
const personalLabels = { untried: '○ 未尝试', failed: '× 尝试未通过', passed: '✓ 已通过' }
const results = computed(() => {
  const q = search.value.trim().toLowerCase()
  const matched = items.value.filter(p => (!q || `${p.id} ${p.title}`.toLowerCase().includes(q)) && (!difficulty.value || p.difficulty === difficulty.value) && (!tag.value || p.tags.includes(tag.value)) && (!status.value || personalStatus(p.id, records.value) === status.value))
  const rank = { 简单: 0, 中等: 1, 困难: 2 }; const stateRank = { untried: 0, failed: 1, passed: 2 }
  return matched.sort((a, b) => {
    let value = sort.value === 'difficulty' ? rank[a.difficulty] - rank[b.difficulty] : sort.value === 'passRate' ? (problemStats(a.id, records.value).passRate ?? -1) - (problemStats(b.id, records.value).passRate ?? -1) : sort.value === 'status' ? stateRank[personalStatus(a.id, records.value)] - stateRank[personalStatus(b.id, records.value)] : a.id.localeCompare(b.id, undefined, { numeric: true })
    return (value || a.id.localeCompare(b.id)) * (direction.value === 'asc' ? 1 : -1)
  })
})
const pagination = computed(() => pageSlice(results.value, page.value, size.value))
watch([search, difficulty, tag, status, size, sort, direction], () => { page.value = 1 })
watch(demoIdentity, enabled => { if (!enabled) status.value = '' })
watch(() => pagination.value.page, current => { page.value = current })
function queryUrl() {
  const q = new URLSearchParams()
  for (const [key, value] of Object.entries({ q: search.value, difficulty: difficulty.value, tag: tag.value, status: status.value, hideTags: hideTags.value ? '1' : '', page: String(page.value), size: String(size.value), sort: sort.value, direction: direction.value })) if (value) q.set(key, value)
  return `/problems?${q}`
}
watch([search, difficulty, tag, status, hideTags, page, size, sort, direction], () => { const url = queryUrl(); rememberListUrl(url); void router.replace(url) })
function clear() { search.value = ''; difficulty.value = ''; tag.value = ''; status.value = ''; page.value = 1 }
function sortBy(column: string) { if (sort.value === column) direction.value = direction.value === 'asc' ? 'desc' : 'asc'; else { sort.value = column; direction.value = 'asc' } }
function arrow(column: string) { return sort.value === column ? direction.value === 'asc' ? '↑' : '↓' : '↕' }
async function load(fail = false) { loading.value = true; error.value = ''; try { items.value = await ojApi.listProblems(fail) } catch (e) { error.value = (e as Error).message } finally { loading.value = false } }
onBeforeRouteLeave(() => { rememberListUrl(queryUrl()); try { sessionStorage.setItem('acm-oj-list-scroll', JSON.stringify({ url: queryUrl(), top: scrollY })) } catch { /* Optional scroll memory. */ } })
onMounted(async () => { document.title = '题库 · 在线评测 · ACM Code Share'; await load(); rememberListUrl(queryUrl()); void router.replace(queryUrl()); await nextTick(); try { const saved = JSON.parse(sessionStorage.getItem('acm-oj-list-scroll') || 'null'); if (saved?.url === queryUrl()) window.scrollTo({ top: saved.top, behavior: 'instant' }) } catch { /* Optional scroll memory. */ } })
const rate = (id: string) => { const value = problemStats(id, records.value).passRate; return value === null ? '—' : `${value.toFixed(1)}%` }
</script>
<template>
  <div class="oj-content-grid">
    <div class="oj-main">
      <header class="oj-title"><h1>在线评测 · 题库</h1><p>选择题目 → 编写代码 → 演示提交流程 → 复盘提升</p></header>
      <div class="oj-filters">
        <label class="oj-search">搜索题目<input v-model="search" type="search" placeholder="按题号或标题搜索，如 P1032、最短路" /></label>
        <label>难度<select v-model="difficulty"><option value="">全部难度</option><option>简单</option><option>中等</option><option>困难</option></select></label>
        <label>算法标签<select v-model="tag"><option value="">全部标签</option><option v-for="item in tags" :key="item">{{ item }}</option></select></label>
        <label>个人状态<select v-model="status" :disabled="!demoIdentity" :title="demoIdentity ? '本机演示状态' : '个人状态需要登录；登录未接入'"><option value="">{{ demoIdentity ? '全部演示状态' : '登录未接入' }}</option><option v-for="(label, key) in personalLabels" :key="key" :value="key">{{ label }}</option></select></label>
        <label class="oj-checkbox"><input v-model="hideTags" type="checkbox" />隐藏标签</label>
      </div>
      <p v-if="!demoIdentity" class="oj-muted">个人状态需要登录；当前登录未接入，启用演示身份可查看本机模拟练习状态。</p>
      <section class="oj-card oj-list-card" :aria-busy="loading">
        <div class="oj-list-heading"><strong>共 {{ loading ? '—' : results.length }} 道题目</strong><button class="oj-link-button" @click="clear">↻ 清除筛选</button></div>
        <div v-if="loading" class="oj-loading" role="status"><span class="oj-spinner"></span>正在加载题库…</div>
        <div v-else-if="error" class="oj-empty" role="alert"><h2>题库加载失败</h2><p>{{ error }}</p><button class="oj-button primary" @click="load()">重试</button></div>
        <div v-else-if="!results.length" class="oj-empty"><h2>没有找到相关题目</h2><p>请调整关键词或筛选条件。</p><button class="oj-button" @click="clear">清除筛选</button></div>
        <template v-else>
          <div class="oj-table-wrap oj-problem-table"><table><thead><tr><th :aria-sort="sort === 'id' ? direction === 'asc' ? 'ascending' : 'descending' : 'none'"><button @click="sortBy('id')">题号 {{ arrow('id') }}</button></th><th>标题</th><th :aria-sort="sort === 'difficulty' ? direction === 'asc' ? 'ascending' : 'descending' : 'none'"><button @click="sortBy('difficulty')">难度 {{ arrow('difficulty') }}</button></th><th v-if="!hideTags">标签</th><th :aria-sort="sort === 'passRate' ? direction === 'asc' ? 'ascending' : 'descending' : 'none'"><button @click="sortBy('passRate')">通过率 {{ arrow('passRate') }}</button></th><th>提交次数</th><th :aria-sort="sort === 'status' ? direction === 'asc' ? 'ascending' : 'descending' : 'none'"><button :disabled="!demoIdentity" @click="sortBy('status')">个人状态 {{ arrow('status') }}</button></th></tr></thead><tbody><tr v-for="p in pagination.items" :key="p.id"><td>{{ p.id }}</td><td><RouterLink :to="`/problem/${p.id}`">{{ p.title }}</RouterLink></td><td><span class="oj-badge" :class="p.difficulty === '简单' ? 'tone-green' : p.difficulty === '中等' ? 'tone-orange' : 'tone-red'">{{ p.difficulty }}</span></td><td v-if="!hideTags"><div class="oj-tags"><span v-for="t in p.tags" :key="t">{{ t }}</span></div></td><td>{{ rate(p.id) }}</td><td>{{ problemStats(p.id, records).submissions }}</td><td><span v-if="demoIdentity" class="oj-personal" :class="personalStatus(p.id, records)">{{ personalLabels[personalStatus(p.id, records)] }}</span><span v-else class="oj-muted">需登录</span></td></tr></tbody></table></div>
          <div class="oj-problem-cards"><div class="oj-mobile-sort"><label>排序<select :value="sort" @change="sortBy(($event.target as HTMLSelectElement).value)"><option value="id">题号</option><option value="difficulty">难度</option><option value="passRate">通过率</option><option value="status" :disabled="!demoIdentity">个人状态</option></select></label><button class="oj-button small" @click="direction = direction === 'asc' ? 'desc' : 'asc'">{{ direction === 'asc' ? '↑ 升序' : '↓ 降序' }}</button></div><article v-for="p in pagination.items" :key="p.id"><div><strong>{{ p.id }}</strong><RouterLink :to="`/problem/${p.id}`">{{ p.title }}</RouterLink></div><div class="oj-tags"><span class="oj-badge">{{ p.difficulty }}</span><template v-if="!hideTags"><span v-for="t in p.tags" :key="t">{{ t }}</span></template></div><p>通过率 {{ rate(p.id) }} · {{ problemStats(p.id, records).submissions }} 次演示提交</p><span v-if="demoIdentity" class="oj-personal" :class="personalStatus(p.id, records)">{{ personalLabels[personalStatus(p.id, records)] }}</span><small v-else>个人状态需要登录</small></article></div>
        </template>
      </section>
      <Pagination v-if="!loading && !error" v-bind="pagination" :size="size" @page="page = $event" @size="size = $event" />
      <p class="oj-muted">通过率 = 演示 AC / 已完成且非系统异常的演示评测；无有效评测显示 —。统计包含预置演示快照与本机演示记录。</p>
      <details class="oj-demo-options"><summary>演示异常场景</summary><p>仅用于验证加载失败与重试，不影响真实服务。</p><button class="oj-button small" @click="load(true)">模拟题库加载失败</button></details>
    </div><Sidebar />
  </div>
</template>
