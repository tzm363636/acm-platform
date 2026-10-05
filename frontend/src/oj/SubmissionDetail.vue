<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ojApi } from './adapter'
import { problems } from './problems'
import { databaseMode } from '../api/database'
import type { Problem } from './types'
import { formatTime, records, submissionsReturnUrl } from './store'
import type { Submission } from './types'
import StatusBadge from './StatusBadge.vue'
import Sidebar from './Sidebar.vue'
import ArticleCodeBlock from '../components/ArticleCodeBlock.vue'
import OjSkeleton from './OjSkeleton.vue'

const route = useRoute(); const id = String(route.params.id)
const submission = ref<Submission>(); const loading = ref(true); const error = ref(''); const feedback = ref('')
const fetchedProblem=ref<Problem>()
const problem = computed(() => databaseMode ? fetchedProblem.value : problems.find(p => p.id === submission.value?.problemId))
watch(submission,async value=>{if(databaseMode&&value)try{fetchedProblem.value=await ojApi.getProblem(value.problemId)}catch{ /* Detail remains usable when the related problem cannot load. */ }})
const finished = computed(() => submission.value?.phase === 'finished')
watch(records, updated => {
  const current = updated.find(record => record.id === id)
  if (current && submission.value && !databaseMode) submission.value = current
})
async function load(fail = false) { loading.value = true; error.value = ''; try { submission.value = await ojApi.getSubmission(id, fail); document.title = `${submission.value ? '演示提交详情' : '提交不存在'} · ACM Code Share` } catch (e) { error.value = (e as Error).message } finally { loading.value = false } }
async function copyId() { try { await navigator.clipboard.writeText(id); feedback.value = '提交编号已复制。' } catch { feedback.value = '复制失败，请手动选择编号复制。' } }
onMounted(() => load())
</script>
<template>
  <OjSkeleton v-if="loading && !submission" detail label="正在加载提交详情…" />
  <section v-else-if="error && !submission" class="oj-card oj-empty" role="alert"><h1>提交详情加载失败</h1><p>{{ error }}</p><button class="oj-button primary" @click="load()">重试</button><RouterLink class="oj-button" :to="submissionsReturnUrl()">返回提交记录</RouterLink></section>
  <section v-else-if="!submission" class="oj-card oj-empty"><h1>提交不存在</h1><p>未找到对应演示提交。请检查链接；其他会话的代码快照需要相应权限。</p><RouterLink class="oj-button primary" :to="submissionsReturnUrl()">返回提交记录</RouterLink></section>
  <template v-else>
    <p class="oj-refresh-status" :role="error ? 'alert' : 'status'">{{ loading ? '正在刷新，保留当前详情…' : error }} <button v-if="error" class="oj-link-button" @click="load()">重试</button></p>
    <div class="oj-inline-links"><RouterLink :to="`/problem/${submission.problemId}`">← 返回题目</RouterLink><RouterLink :to="submissionsReturnUrl()">返回提交记录 →</RouterLink></div>
    <header class="oj-title"><h1>提交详情 / 评测结果</h1><p>本次演示提交的结果与代码快照 · 用户代码未执行</p></header>
    <section class="oj-card oj-verdict-summary"><div class="oj-verdict-large"><StatusBadge :verdict="submission.verdict" /><strong>{{ submission.verdict }}</strong><span>{{ submission.origin === 'fixture' ? '预置演示快照' : '本机演示提交' }}</span></div><dl><div><dt>提交编号</dt><dd class="oj-wrap-id">{{ submission.id }} <button class="oj-button small" aria-label="复制提交编号" @click="copyId">复制</button></dd></div><div><dt>题目</dt><dd><RouterLink :to="`/problem/${submission.problemId}`">{{ submission.problemId }} {{ problem?.title }}</RouterLink></dd></div><div><dt>提交者</dt><dd>{{ submission.user }}</dd></div><div><dt>语言 / 编译器版本</dt><dd>C++17 · 编译器未接入（未实际编译）</dd></div><div><dt>提交时间</dt><dd>{{ formatTime(submission.submittedAt) }}</dd></div><div><dt>评测完成时间</dt><dd>{{ formatTime(submission.finishedAt) }}</dd></div><div><dt>运行耗时（演示）</dt><dd>{{ submission.timeMs === null ? '—' : `${submission.timeMs} ms` }}</dd></div><div><dt>内存（演示）</dt><dd>{{ submission.memoryMB === null ? '—' : `${submission.memoryMB} MB` }}</dd></div></dl><p class="oj-feedback" role="status">{{ feedback }}</p></section>
    <div class="oj-content-grid"><div class="oj-main oj-detail-stack"><section class="oj-card"><div class="oj-sample-title"><h2>提交时的代码快照</h2><RouterLink v-if="submission.code !== undefined" class="oj-button" :to="`/problem/${submission.problemId}?resume=${submission.id}`">✎ 继续修改</RouterLink><button v-else class="oj-button" disabled title="其他演示用户的源码不可访问">继续修改（无源码权限）</button></div><ArticleCodeBlock v-if="submission.code !== undefined" :code="submission.code" label="提交快照" /><p v-else class="oj-muted">当前记录不允许访问源码，前端数据中不包含该用户的代码。</p><p class="oj-muted">快照独立保存，后续修改草稿不会改变本次提交的代码。</p></section>
      <section class="oj-card"><h2>公开测试点结果 <small>固定演示摘要</small></h2><div v-if="submission.cases.length" class="oj-table-wrap"><table><thead><tr><th>测试点</th><th>结果</th><th>耗时（演示）</th><th>内存（演示）</th></tr></thead><tbody><tr v-for="c in submission.cases" :key="c.name"><td>{{ c.name }}</td><td><StatusBadge :verdict="c.verdict" /></td><td>{{ c.timeMs === null ? '—' : `${c.timeMs} ms` }}</td><td>{{ c.memoryMB === null ? '—' : `${c.memoryMB} MB` }}</td></tr></tbody></table></div><p v-else class="oj-muted">{{ !finished ? '评测未完成，没有测试点结果。' : submission.verdict === 'CE' ? '编译失败，没有运行测试点。' : '此记录未提供公开测试点结果。' }}</p><p class="oj-warning">仅展示公开摘要；不包含隐藏测试的输入或标准答案。演示摘要不代表真实判题。</p></section>
      <details class="oj-card oj-information" open><summary>编译 / 运行信息</summary><pre>{{ submission.information }}</pre></details>
      <div class="oj-action-buttons"><RouterLink class="oj-button primary" :to="`/problem/${submission.problemId}`">返回题目</RouterLink><RouterLink class="oj-button" :to="submissionsReturnUrl()">查看提交记录</RouterLink></div>
      <details class="oj-demo-options"><summary>演示异常场景</summary><button class="oj-button small" @click="load(true)">模拟详情加载失败</button></details>
    </div><Sidebar :article-ids="problem?.articleIds || []" /></div>
  </template>
</template>
