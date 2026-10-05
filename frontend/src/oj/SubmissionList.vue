<script setup lang="ts">
import UiIcon from '../components/UiIcon.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ojApi, pageSlice } from './api'
import { demoIdentity, enableDemoIdentity, formatTime, records, rememberSubmissionsUrl, syncRecords } from './store'
import { LOCAL_USER, verdicts, type Verdict } from './types'
import { problems } from './problems'
import Sidebar from './Sidebar.vue'
import StatusBadge from './StatusBadge.vue'
import Pagination from './Pagination.vue'
import OjSkeleton from './OjSkeleton.vue'

const route = useRoute(); const router = useRouter(); const hasLoaded = ref(false)
const search = ref(String(route.query.q || '')); const language = ref(String(route.query.language || ''))
const verdict = ref(String(route.query.verdict || '')); const user = ref(String(route.query.user || ''))
const scope = ref(route.query.scope === 'mine' ? 'mine' : 'all'); const loading = ref(true); const error = ref('')
const page = ref(Math.max(1, Number(route.query.page) || 1)); const size = ref([4,8,16].includes(Number(route.query.size)) ? Number(route.query.size) : 8)
const sort = ref(route.query.sort === 'id' ? 'id' : 'time'); const direction = ref(route.query.direction === 'asc' ? 'asc' : 'desc')
const problemOf = (id: string) => problems.find(p => p.id === id)
const users = computed(() => [...new Set(records.value.map(s => s.user))])
const results = computed(() => {
  const q = search.value.trim().toLowerCase()
  return records.value.filter(s => (scope.value === 'all' || s.user === LOCAL_USER) && (!q || `${s.problemId} ${problemOf(s.problemId)?.title}`.toLowerCase().includes(q)) && (!language.value || language.value === s.language) && (!verdict.value || verdict.value === s.verdict) && (!user.value || user.value === s.user)).sort((a,b) => ((sort.value === 'id' ? a.id.localeCompare(b.id, undefined, { numeric: true }) : a.submittedAt.localeCompare(b.submittedAt)) || a.id.localeCompare(b.id)) * (direction.value === 'asc' ? 1 : -1))
})
const pagination = computed(() => pageSlice(results.value, page.value, size.value))
watch([search, language, verdict, user, scope, size, sort, direction], () => { page.value = 1 })
watch(() => pagination.value.page, value => { page.value = value })
watch([search, language, verdict, user, scope, page, size, sort, direction], () => {
  const query = new URLSearchParams()
  for (const [key,value] of Object.entries({ q: search.value, language: language.value, verdict: verdict.value, user: user.value, scope: scope.value, page: String(page.value), size: String(size.value), sort: sort.value, direction: direction.value })) if (value) query.set(key, value)
  const url = `/submissions?${query}`; rememberSubmissionsUrl(url); void router.replace(url)
})
function clear() { search.value = ''; language.value = ''; verdict.value = ''; user.value = ''; page.value = 1 }
function sortBy(column: string) { if (sort.value === column) direction.value = direction.value === 'asc' ? 'desc' : 'asc'; else { sort.value = column; direction.value = 'asc' } }
const arrow = (column: string) => sort.value === column ? direction.value === 'asc' ? '↑' : '↓' : '↕'
async function load(fail = false) { loading.value = true; error.value = ''; try { await ojApi.listSubmissions(fail); syncRecords(); hasLoaded.value = true } catch (e) { error.value = (e as Error).message } finally { loading.value = false } }
onMounted(() => { document.title = '提交记录 · 在线评测 · ACM Code Share'; rememberSubmissionsUrl(route.fullPath); void load() })
</script>
<template>
  <div class="oj-content-grid"><div class="oj-main">
    <header class="oj-title"><h1>提交记录</h1><p>查看预置与本机演示提交；不包含真实评测数据</p></header>
    <div class="oj-segmented" aria-label="提交记录范围"><button :aria-pressed="scope === 'all'" :class="{active:scope === 'all'}" @click="scope = 'all'"><UiIcon name="globe" /> 全站提交（演示）</button><button :aria-pressed="scope === 'mine'" :class="{active:scope === 'mine'}" @click="scope = 'mine'"><UiIcon name="user" /> 我的提交{{ demoIdentity ? '（演示）' : '' }}</button></div>
    <section v-if="scope === 'mine' && !demoIdentity" class="oj-card oj-empty"><h2>个人提交记录需要登录</h2><p>登录功能未接入。可以启用“本机演示用户”，查看仅在本机保存的演示记录。</p><button class="oj-button primary" @click="enableDemoIdentity">查看本机演示提交</button><button class="oj-button" disabled>真实登录（未接入）</button></section>
    <template v-else>
      <div class="oj-filters"><label class="oj-search">搜索题号或标题<input v-model="search" type="search" placeholder="如 P1032、最短路" /></label><label>语言<select v-model="language"><option value="">全部语言</option><option value="cpp17">C++17</option><option disabled>Python（规划中）</option><option disabled>Java（规划中）</option></select></label><label>评测结果<select v-model="verdict"><option value="">全部结果</option><option v-for="(info,key) in verdicts" :key="key" :value="key">{{ key }} {{ info.label }}</option></select></label><label>提交者<select v-model="user"><option value="">全部演示用户</option><option v-for="name in users" :key="name">{{ name }}</option></select></label><button class="oj-link-button" @click="clear">↻ 清除筛选</button></div>
      <section class="oj-card oj-list-card" :aria-busy="loading"><div class="oj-list-heading"><strong>共 {{ loading && !hasLoaded ? '—' : results.length }} 条演示记录</strong><button class="oj-link-button" :disabled="loading" @click="load()">刷新</button></div>
        <p class="oj-refresh-status" :role="error ? 'alert' : 'status'">{{ hasLoaded && loading ? '正在刷新，保留当前记录…' : hasLoaded && error ? error : '' }} <button v-if="hasLoaded && error" class="oj-link-button" @click="load()">重试</button></p><OjSkeleton v-if="loading && !hasLoaded" label="正在加载提交记录…" :rows="size" /><div v-else-if="error && !hasLoaded" class="oj-empty" role="alert"><h2>记录加载失败</h2><p>{{ error }}</p><button class="oj-button primary" @click="load()">重试</button></div>
        <div v-else-if="!results.length" class="oj-empty"><h2>{{ search || language || verdict || user ? '没有符合筛选条件的提交' : '暂无提交记录' }}</h2><p>可以清除筛选，或去题库体验一次演示提交。</p><button v-if="search || language || verdict || user" class="oj-button" @click="clear">清除筛选</button><RouterLink class="oj-button primary" to="/problems">前往题库 →</RouterLink></div>
        <div v-else class="oj-table-wrap oj-submission-table"><table><thead><tr><th :aria-sort="sort === 'id' ? direction === 'asc' ? 'ascending' : 'descending' : 'none'"><button @click="sortBy('id')">提交编号 {{ arrow('id') }}</button></th><th>题目</th><th>用户</th><th>语言</th><th>结果</th><th>耗时</th><th>内存</th><th :aria-sort="sort === 'time' ? direction === 'asc' ? 'ascending' : 'descending' : 'none'"><button @click="sortBy('time')">提交时间 {{ arrow('time') }}</button></th><th>操作</th></tr></thead><tbody><tr v-for="s in pagination.items" :key="s.id"><td><span class="oj-submission-id" :title="s.id">{{ s.origin === 'local' ? `${s.id.slice(0,14)}…` : s.id }}</span></td><td><RouterLink :to="`/problem/${s.problemId}?from=submissions`">{{ s.problemId }} {{ problemOf(s.problemId)?.title }}</RouterLink></td><td>{{ s.user }}</td><td>C++17</td><td><StatusBadge :verdict="s.verdict as Verdict" /></td><td>{{ s.timeMs === null ? '—' : `${s.timeMs} ms` }}</td><td>{{ s.memoryMB === null ? '—' : `${s.memoryMB} MB` }}</td><td>{{ formatTime(s.submittedAt) }}</td><td><RouterLink :to="`/submission/${s.id}`">查看详情</RouterLink></td></tr></tbody></table></div>
      </section><Pagination v-if="hasLoaded || (!loading && !error)" v-bind="pagination" :size="size" @page="page = $event" @size="size = $event" />
      <p class="oj-muted">预置演示快照用于展示各种状态。Pending / Judging 未完成，因此耗时、内存与完成时间显示 —；本机的新演示提交由明确的模拟步骤驱动。</p>
    </template>
    <details class="oj-demo-options"><summary>演示异常场景</summary><button class="oj-button small" @click="load(true)">模拟记录加载失败</button></details>
  </div><Sidebar /></div>
</template>
