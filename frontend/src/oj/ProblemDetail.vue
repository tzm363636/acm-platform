<script setup lang="ts">
import UiIcon from '../components/UiIcon.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ojApi } from './adapter'
import { enableDemoIdentity, listReturnUrl, submissionsReturnUrl, syncRecords, storageNotice } from './store'
import { scenarios, type Problem, type RunResult, type Scenario, type Submission } from './types'
import { articles, articleUrl, type Article } from '../data/articles'
import { databaseMode, getData } from '../api/database'
import CodeEditor from './CodeEditor.vue'
import StatusBadge from './StatusBadge.vue'
import OjSkeleton from './OjSkeleton.vue'

const route = useRoute(); const router = useRouter(); const id = String(route.params.id)
const backUrl = route.query.from === 'submissions' ? submissionsReturnUrl() : listReturnUrl()
const backLabel = route.query.from === 'submissions' ? '返回提交记录' : '返回题库'
const problem = ref<Problem>(); const loading = ref(true); const loadError = ref('')
const code = ref(''); const input = ref(''); const sampleIndex = ref(0); const tab = ref('problem')
const scenario = ref<Scenario>('WA'); const result = ref<RunResult>(); const outputTab = ref('output')
const running = ref(false); const submitting = ref(false); const actionError = ref(''); const currentSubmission = ref<Submission>()
const copyFeedback = ref(''); const draftStatus = ref('')
const lastAction = ref<'run' | 'submit'>('run')
const busy = computed(() => running.value || submitting.value)
const emptyCode = computed(() => !code.value.trim())
const fetchedArticles=ref<Article[]>([])
const related = computed(() => databaseMode ? fetchedArticles.value : articles.filter(a => problem.value?.articleIds.includes(a.id)))
watch(problem, async value=>{if(databaseMode&&value){const responses=await Promise.allSettled(value.articleIds.map(id=>getData<Article>(`/articles/${id}`)));fetchedArticles.value=responses.filter((r):r is PromiseFulfilledResult<Article>=>r.status==='fulfilled').map(r=>r.value)}})
const phases = ['waiting', 'compiling', 'judging', 'finished']; const phaseLabels = ['等待', '编译', '评测', '完成']
const phaseIndex = computed(() => currentSubmission.value ? phases.indexOf(currentSubmission.value.phase) : -1)
let ready = false
watch(code, value => { if (ready) { const saved = ojApi.saveDraft(id, 'cpp17', value); draftStatus.value = saved ? '草稿已保存到本机（按题目 / C++17）' : '草稿保存失败，请复制备份。'; storageNotice.value = ojApi.notice } })
watch(input, value => { if (ready) ojApi.saveDraft(id, 'test-input', value) })
async function load(fail = false) {
  loading.value = true; loadError.value = ''
  try { problem.value = await ojApi.getProblem(id, fail); if (problem.value && !ready) {
    code.value = ojApi.getDraft(id, 'cpp17', problem.value.template); input.value = ojApi.getDraft(id, 'test-input', '')
    draftStatus.value = '本机草稿已恢复；初次访问使用起始模板。'; ready = true
    if (typeof route.query.resume === 'string') {
      const snapshot = await ojApi.getSubmission(route.query.resume)
      if (snapshot?.problemId === id && snapshot.code !== undefined) {
        if (code.value !== snapshot.code && code.value !== problem.value.template && !window.confirm('继续修改会覆盖当前不同的本机草稿。是否使用本次提交的代码快照？')) draftStatus.value = '已保留原草稿，未覆盖。'
        else { code.value = snapshot.code; ojApi.saveDraft(id, 'cpp17', code.value); draftStatus.value = '已载入提交时的代码快照。'; tab.value = 'code' }
      } else actionError.value = '提交快照不存在、无源码访问权限或不属于当前题目。已保留草稿。'
      const query = { ...route.query }; delete query.resume; void router.replace({ path: route.path, query })
    }
    storageNotice.value = ojApi.notice; document.title = `${problem.value.id} ${problem.value.title} · 在线评测`
  } } catch (e) { loadError.value = (e as Error).message } finally { loading.value = false }
}
async function copySample(value: string, name: string) { try { await navigator.clipboard.writeText(value); copyFeedback.value = `${name}已复制。` } catch { copyFeedback.value = `${name}复制失败，请手动选择后复制。` } }
function useSample() { input.value = problem.value!.samples[sampleIndex.value]!.input; copyFeedback.value = `已将样例 ${sampleIndex.value + 1} 放入测试输入。` }
async function run() {
  if (busy.value || emptyCode.value || !problem.value) return
  lastAction.value = 'run'; running.value = true; actionError.value = ''; result.value = undefined; outputTab.value = 'output'; tab.value = 'result'
  try { result.value = await ojApi.run(problem.value, code.value, input.value, scenario.value); if (result.value.verdict === 'CE') outputTab.value = 'information' }
  catch (e) { actionError.value = (e as Error).message } finally { running.value = false }
}
async function submit() {
  if (busy.value || emptyCode.value || !problem.value) return
  lastAction.value = 'submit'; submitting.value = true; actionError.value = ''; currentSubmission.value = undefined; tab.value = 'result'
  const snapshot = code.value; enableDemoIdentity()
  try { currentSubmission.value = await ojApi.submit(id, snapshot, scenario.value, record => { currentSubmission.value = record; syncRecords() }); syncRecords() }
  catch (e) { actionError.value = (e as Error).message } finally { submitting.value = false }
}
function retry() { void (lastAction.value === 'submit' ? submit() : run()) }
onMounted(() => load())
</script>
<template>
  <OjSkeleton v-if="loading && !problem" detail label="正在加载题目…" />
  <section v-else-if="loadError && !problem" class="oj-card oj-empty" role="alert"><h1>题目加载失败</h1><p>{{ loadError }}</p><button class="oj-button primary" @click="load()">重试</button><RouterLink class="oj-button" :to="listReturnUrl()">返回题库</RouterLink></section>
  <section v-else-if="!problem" class="oj-card oj-empty"><h1>题目不存在</h1><p>未找到题号 {{ id }}，请检查链接。</p><RouterLink class="oj-button primary" :to="listReturnUrl()">返回题库</RouterLink></section>
  <template v-else>
    <div class="oj-detail-heading"><RouterLink class="oj-button small" :to="backUrl">← {{ backLabel }}</RouterLink><div><h1>{{ problem.id }} {{ problem.title }}</h1><span class="oj-badge tone-orange">{{ problem.difficulty }}</span></div><p>◷ 时间限制 {{ problem.timeLimit }} ms　▣ 内存限制 {{ problem.memoryLimit }} MB　来源：{{ problem.source }}</p><div class="oj-inline-links"><a v-for="a in related" :key="a.id" :href="articleUrl(a.id)">相关题解：{{ a.title }} ↗</a><span v-if="!related.length" class="oj-muted">暂无对应题解</span><RouterLink :to="`/submissions?q=${problem.id}`">当前题目提交记录 →</RouterLink></div></div>
    <p class="oj-refresh-status" :role="loadError ? 'alert' : 'status'">{{ loading ? '正在刷新，保留题目与草稿…' : loadError }} <button v-if="loadError" class="oj-link-button" @click="load()">重试</button></p>
    <div class="oj-mobile-tabs" role="tablist" aria-label="题目工作区"><button v-for="(label, key) in {problem:'题目',code:'代码',result:'结果'}" :id="`tab-${key}`" :key="key" role="tab" :aria-selected="tab === key" :aria-controls="`panel-${key}`" :tabindex="tab === key ? 0 : -1" :class="{ active: tab === key }" @click="tab = key" @keydown.left.prevent="tab = key === 'problem' ? 'result' : key === 'code' ? 'problem' : 'code'; ($event.currentTarget as HTMLElement).parentElement?.querySelector<HTMLButtonElement>(`#tab-${tab}`)?.focus()" @keydown.right.prevent="tab = key === 'result' ? 'problem' : key === 'problem' ? 'code' : 'result'; ($event.currentTarget as HTMLElement).parentElement?.querySelector<HTMLButtonElement>(`#tab-${tab}`)?.focus()">{{ label }}</button></div>
    <div class="oj-workspace" :class="`mobile-tab-${tab}`">
      <section id="panel-problem" class="oj-card oj-problem-panel" aria-labelledby="tab-problem">
        <details open><summary>题目描述</summary><p>{{ problem.description }}</p></details><details open><summary>输入格式</summary><p>{{ problem.input }}</p></details><details open><summary>输出格式</summary><p>{{ problem.output }}</p></details>
        <details open><summary>输入输出样例</summary><div v-for="(sample, index) in problem.samples" :key="index" class="oj-sample-group"><div class="oj-sample-pair"><div><div class="oj-sample-title"><strong>样例输入 {{ index + 1 }}</strong><button class="oj-button small" :aria-label="`复制样例输入 ${index+1}`" @click="copySample(sample.input, `样例输入 ${index+1}`)">复制</button></div><pre>{{ sample.input }}</pre></div><div><div class="oj-sample-title"><strong>样例输出 {{ index + 1 }}</strong><button class="oj-button small" :aria-label="`复制样例输出 ${index+1}`" @click="copySample(sample.output, `样例输出 ${index+1}`)">复制</button></div><pre>{{ sample.output }}</pre></div></div><p><strong>样例解释：</strong>{{ sample.explanation }}</p></div><p class="oj-feedback" role="status">{{ copyFeedback }}</p></details>
        <details open><summary>数据范围与提示</summary><ul><li v-for="constraint in problem.constraints" :key="constraint">{{ constraint }}</li></ul></details>
      </section>
      <div class="oj-editor-column">
        <section id="panel-code" class="oj-card oj-code-panel" aria-labelledby="tab-code"><CodeEditor v-model="code" :template="problem.template" /><p class="oj-muted" role="status">{{ draftStatus }}</p><p v-if="emptyCode" class="oj-warning" role="status">代码为空，请先输入代码；运行与提交已禁用。</p></section>
        <section class="oj-card oj-input-panel"><div class="oj-sample-title"><h2>自定义输入</h2><div class="oj-sample-actions"><select v-model="sampleIndex" aria-label="选择测试样例"><option v-for="(_,index) in problem.samples" :key="index" :value="index">样例 {{ index+1 }}</option></select><button class="oj-button small" @click="useSample">使用样例</button><button class="oj-button small" @click="input = ''">清空输入</button></div></div><textarea v-model="input" aria-label="自定义测试输入" placeholder="输入自定义测试数据，或使用题目样例" rows="5"></textarea></section>
        <section class="oj-card oj-action-panel"><label>固定演示场景<select v-model="scenario" :disabled="busy"><option v-for="s in scenarios" :key="s.value" :value="s.value">{{ s.label }}</option></select></label><p class="oj-muted">所选结果与用户代码无关；AC 场景仅更新演示练习状态。自定义输入不会实际执行。</p><div class="oj-action-buttons"><button class="oj-button" :disabled="busy || emptyCode" @click="run"><UiIcon name="play" /> {{ running ? '模拟运行中…' : '运行测试（演示）' }}</button><button class="oj-button primary" :disabled="busy || emptyCode" @click="submit"><UiIcon name="send" /> {{ submitting ? '演示提交中…' : '提交评测（演示）' }}</button></div><p class="oj-muted">正式提交需要登录与判题接口。</p><button class="oj-button small" disabled title="登录与真实判题服务未接入">正式提交（未接入）</button></section>
        <section id="panel-result" class="oj-card oj-result-panel" aria-labelledby="tab-result" aria-live="polite">
          <h2>运行与提交结果 <small>演示</small></h2>
          <div v-if="currentSubmission" class="oj-submission-progress content-reveal"><ol class="oj-steps"><li v-for="(phase,index) in phases" :key="phase" :class="{ reached: phaseIndex >= index }"><span>{{ phaseIndex > index ? '✓' : index+1 }}</span>{{ phaseLabels[index] }}</li></ol><div class="oj-result-heading"><StatusBadge :verdict="currentSubmission.verdict" /><span>{{ currentSubmission.id }}</span></div><p>{{ currentSubmission.information }}</p><RouterLink class="oj-button small" :to="`/submission/${currentSubmission.id}`">查看提交详情 →</RouterLink></div>
          <div v-if="running" class="oj-loading" role="status"><span class="oj-spinner"></span>正在展示固定运行场景，未执行代码…</div>
          <div v-if="actionError" class="oj-error" role="alert"><strong>请求失败</strong><p>{{ actionError }}</p><button class="oj-button small" :disabled="busy || emptyCode" @click="retry">重试</button></div>
          <div v-if="result && !running" class="oj-run-result content-reveal"><div class="oj-result-heading"><span v-if="result.verdict === 'AC'" class="oj-badge tone-green">✓ 演示运行完成</span><StatusBadge v-else :verdict="result.verdict" /><span v-if="result.matchedSample">演示样例运行通过，仅代表固定样例展示；用户代码未执行。</span><span v-else-if="result.verdict === 'AC'">演示运行流程成功；没有预期输出对比，不判断答案正确。</span></div><p class="oj-muted">演示耗时 {{ result.timeMs === null ? '—' : `${result.timeMs} ms` }} · 演示内存 {{ result.memoryMB === null ? '—' : `${result.memoryMB} MB` }} · 不计入提交记录</p><div class="oj-output-tabs" role="tablist" aria-label="测试结果内容"><button v-for="(label,key) in {output:'测试输出',expected:'预期输出',information:'编译／运行信息'}" :id="`output-tab-${key}`" :key="key" role="tab" :aria-selected="outputTab === key" :aria-controls="`output-${key}`" :class="{ active: outputTab === key }" @click="outputTab = key">{{ label }}</button></div><pre :key="outputTab" class="content-reveal" :id="`output-${outputTab}`" role="tabpanel" :aria-labelledby="`output-tab-${outputTab}`">{{ outputTab === 'output' ? result.output || '此场景没有标准输出。' : outputTab === 'expected' ? result.expected ?? '当前输入未匹配题目公开样例，没有预期输出，不进行正确性判断。' : result.information }}</pre></div>
          <p v-if="!result && !currentSubmission && !running && !actionError" class="oj-muted">尚未运行或提交。选择固定演示场景，再开始体验流程。</p>
        </section>
      </div>
    </div>
    <details class="oj-demo-options"><summary>演示异常场景</summary><button class="oj-button small" :disabled="busy" @click="load(true)">模拟题目加载失败</button></details>
  </template>
</template>
