<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import ArticleCodeBlock from '../components/ArticleCodeBlock.vue'
import { codeLanguage, codeLanguages } from '../components/codeLanguages'
import { ApiError, getData, postData, putData } from '../api/database'
import { authStatus, currentUser } from './auth'
import { chooseAction, confirmAction } from './confirmation'
import { registerLeaveGuard, requestLeave } from './leaveGuard'
import { DraftSaver, type SaveState } from './draftSaver'
import { contentSignature as fingerprint } from './contentSignature'
import { statusNames, time, type ManagedArticle, type Options } from './types'
import type { ArticleSection } from '../data/articles'
type Form = { title: string; summary: string; categoryId: number; tagIds: number[]; sections: ArticleSection[]; revision: number; draftKey?: string }
const route = useRoute(), router = useRouter(), id = ref(route.params.id ? Number(route.params.id) : undefined)
const owner = currentUser.value?.id, legacyBackupKey = `acm-article-draft:${owner}:${id.value || 'new'}`
let tabId: string
try { tabId = sessionStorage.getItem('acm-writing-tab') || crypto.randomUUID(); sessionStorage.setItem('acm-writing-tab', tabId) } catch { tabId = crypto.randomUUID() }
// Copy-on-write backups also isolate a browser's "duplicate tab", which clones sessionStorage.
const pointerKey = `${legacyBackupKey}:pointer`, backupKey = `${legacyBackupKey}:snapshot:${crypto.randomUUID()}`
let restoreKey: string | null = `${legacyBackupKey}:tab:${tabId}`
try { restoreKey = sessionStorage.getItem(pointerKey) || restoreKey } catch {}
const article = ref<ManagedArticle>(), options = ref<Options>({ categories: [], tags: [] }), loading = ref(true), busy = ref(false), error = ref(''), notice = ref('')
const preview = ref(route.path.startsWith('/admin/articles/')), reason = ref(''), dirty = ref(false), tagSearch = ref(''), validation = ref(''), freeze = ref(false)
const saveState = ref<SaveState>('unsaved'), lastSavedAt = ref<string>(), serverVersion = ref<ManagedArticle>(), showServer = ref(false), copyFeedback = ref('')
const newSection = (): ArticleSection => ({ heading: '', level: 2, paragraphs: [''], bullets: [], code: '', codeLanguage: 'plaintext' })
const form = ref<Form>({ title: '', summary: '', categoryId: 0, tagIds: [], sections: [{ ...newSection(), heading: '正文' }], revision: 0, draftKey: crypto.randomUUID() })
const admin = computed(() => authStatus.value === 'authenticated' && currentUser.value?.id === owner && currentUser.value?.role === 'ADMIN')
const editable = computed(() => authStatus.value === 'authenticated' && currentUser.value?.id === owner && article.value?.status !== 'PENDING' && (!article.value || admin.value || ['DRAFT', 'REJECTED'].includes(article.value.status)))
const autoAllowed = computed(() => editable.value && !busy.value && !serverVersion.value && (!article.value || ['DRAFT', 'REJECTED'].includes(article.value.status)))
const selectedTags = computed(() => options.value.tags.filter(t => form.value.tagIds.includes(t.id)))
const visibleTags = computed(() => options.value.tags.filter(t => !form.value.tagIds.includes(t.id) && t.name.toLowerCase().includes(tagSearch.value.trim().toLowerCase())))
const returnTo = computed(() => { const value = String(route.query.list || ''); return /^\/(admin|articles)(\?|$)/.test(value) && !value.includes('\\') ? value : route.path.startsWith('/admin') ? '/admin' : '/articles' })
let applying = false, backupSafe = false, savedFingerprint = ''
function normalize(s: ArticleSection): ArticleSection { return { ...s, level: s.level || 2, paragraphs: [...s.paragraphs], bullets: [...(s.bullets || [])], code: s.code || '', codeLanguage: codeLanguage(s.codeLanguage).value } }
function articleForm(a: ManagedArticle): Form { return { title: a.title, summary: a.summary, categoryId: a.categoryId, tagIds: [...a.tagIds], sections: a.sections.map(normalize), revision: a.revision } }
function backup() { if (!owner) return; try { localStorage.setItem(backupKey, JSON.stringify({ form: form.value, articleId: id.value, lastSavedAt: lastSavedAt.value })); sessionStorage.setItem(pointerKey, backupKey); restoreKey = backupKey; backupSafe = true } catch { backupSafe = false; notice.value = '本地备份不可用，请及时保存到服务器。' } }
function clearBackup() { try { localStorage.removeItem(backupKey); sessionStorage.removeItem(pointerKey); localStorage.removeItem(legacyBackupKey); restoreKey = null } catch {} }
function apply(a: ManagedArticle) { applying = true; article.value = a; id.value = a.id; form.value = articleForm(a); savedFingerprint = fingerprint(form.value); dirty.value = false; applying = false; serverVersion.value = undefined; showServer.value = false; saver.reset() }
function validate() {
  if (!form.value.title.trim() || !form.value.summary.trim() || !form.value.categoryId) return '填写标题、摘要并选择分类后才会保存到服务器；当前内容保留在本机。'
  if (!form.value.sections.length || form.value.sections.length > 40) return '正文需要 1–40 个章节。'
  const html = /<\s*\/?\s*[a-zA-Z][^>]*>|javascript\s*:|on[a-z]+\s*=/i
  if ([form.value.title, form.value.summary, ...form.value.sections.flatMap(s => [s.heading, ...s.paragraphs, ...(s.bullets || [])])].some(v => html.test(v))) return '标题和正文请使用纯文本，HTML 示例放入代码块。'
  let length = 0
  for (const s of form.value.sections) {
    if (!s.heading.trim() || s.heading.length > 160) return '每个章节需要不超过 160 字的标题。'
    if (s.paragraphs.length > 100 || (s.bullets?.length || 0) > 100) return '每章段落和列表最多各 100 项。'
    if (s.paragraphs.some(p => !p.trim() || p.length > 12000) || s.bullets?.some(p => !p.trim() || p.length > 2000)) return '请填写或删除空段落；每段最多 12000 字，每个列表项最多 2000 字。'
    if ((s.code?.length || 0) > 100000 || s.code?.includes('\0')) return '代码块最多 100000 字，不能包含空字符。'
    length += [...s.paragraphs, ...(s.bullets || []), s.code || ''].join('').length
  }
  return !length || length > 500000 ? '正文不能为空，且总长度不超过 500000 字。' : ''
}
const saver = new DraftSaver<Form, ManagedArticle>({
  dirty: () => dirty.value, autoAllowed: () => autoAllowed.value,
  capture: () => { if (!editable.value || serverVersion.value) return undefined; validation.value = validate(); return validation.value ? undefined : JSON.parse(JSON.stringify(form.value)) },
  send: snapshot => { if (!editable.value) throw new ApiError('身份尚未确认或已变化，保存已暂停。'); return id.value ? putData(`/account/articles/${id.value}`, snapshot) : postData('/account/articles', snapshot) },
  accept: (a, snapshot) => {
    if (authStatus.value !== 'authenticated' || currentUser.value?.id !== owner) { backup(); saver.pause(); notice.value = '账户状态变化，旧保存响应没有覆盖当前输入。请重新确认身份后检查服务器版本。'; return }
    // Acknowledge a snapshot without applying its old body over newer typing.
    if (!id.value && a.revision > 0) { article.value = a; id.value = a.id; serverVersion.value = a; saver.pause(); error.value = '创建请求已成功，但服务器草稿随后被更新。双方内容已保留，请处理版本冲突。'; backup(); return }
    article.value = a; id.value = a.id; applying = true; form.value.revision = a.revision; form.value.draftKey ||= snapshot.draftKey; applying = false
    savedFingerprint = fingerprint(articleForm(a)); dirty.value = fingerprint(form.value) !== savedFingerprint; lastSavedAt.value = new Date().toISOString(); error.value = ''; notice.value = ''; backup()
  },
  report: (state, failure) => { saveState.value = serverVersion.value ? 'failed' : state; if (failure) error.value = (failure as Error).message },
  failed: failure => { backup(); if (failure instanceof ApiError && failure.status === 409) void refreshConflict() }
})
watch(form, () => { if (!loading.value && !applying) { dirty.value = fingerprint(form.value) !== savedFingerprint; validation.value = ''; backup(); saver.edited() } }, { deep: true, flush: 'sync' })
watch(editable, value => { if (!value) { if (dirty.value) backup(); saver.pause() } else if (dirty.value) notice.value = '身份已确认，未保存内容仍在。请手动保存或查看服务器版本。' }, { flush: 'sync' })
watch(currentUser, user => { if (user && article.value?.authorId === user.id) article.value.author = user.displayName })
async function load() {
  if (dirty.value && !await confirmAction('重新读取服务器内容？本地输入仍会保留在备份中。')) return
  loading.value = true; error.value = ''; saver.pause()
  try {
    options.value = await getData('/account/options')
    let stored: { form: Form; articleId?: number; lastSavedAt?: string } | undefined
    try { const raw = (restoreKey && localStorage.getItem(restoreKey)) || localStorage.getItem(legacyBackupKey); if (raw) { const value = JSON.parse(raw); stored = value.form ? value : { form: value } } } catch { notice.value = '本机备份无法读取，服务器内容仍会保留。' }
    if (!id.value && stored?.articleId) id.value = stored.articleId
    if (id.value) apply(await getData<ManagedArticle>(`/account/articles/${id.value}`))
    else { applying = true; form.value.categoryId = options.value.categories[0]?.id || 0; applying = false; savedFingerprint = fingerprint(form.value); dirty.value = false; saver.reset(); saveState.value = 'unsaved' }
    if (stored && editable.value) {
      const server = article.value; applying = true; form.value = { ...stored.form, sections: stored.form.sections.map(normalize) }; applying = false
      if (!id.value) form.value.draftKey ||= crypto.randomUUID()
      dirty.value = fingerprint(form.value) !== savedFingerprint; backupSafe = true; lastSavedAt.value = stored.lastSavedAt
      if (server && stored.form.revision !== server.revision && dirty.value) { serverVersion.value = server; saver.pause(); saveState.value = 'failed'; error.value = '本机和服务器版本不同，已暂停自动保存。请选择如何处理。' }
      else { if (server) form.value.revision = server.revision; notice.value = dirty.value ? '已恢复本机未保存内容。' : '草稿已从服务器恢复。'; saver.reset() }
    }
  } catch (e) { error.value = (e as Error).message } finally { loading.value = false; if (dirty.value && !serverVersion.value) saver.edited() }
}
async function save() {
  if (busy.value || !editable.value || serverVersion.value) return
  if (!id.value && !dirty.value) { validation.value = validate(); saveState.value = 'unsaved'; return }
  if (article.value?.status === 'PUBLISHED' && !await confirmAction('保存后将立即更新公开文章的正文、标题和摘要，所有访客都会看到。确认保存并更新公开文章？')) return
  busy.value = true; freeze.value = article.value?.status === 'PUBLISHED'; error.value = ''
  try { if (await saver.flush()) { notice.value = article.value?.status === 'PUBLISHED' ? '公开文章已更新。' : '草稿已保存到数据库。'; if (!route.params.id && id.value && !dirty.value) { freeze.value = true; clearBackup(); await router.replace({ path: `/articles/${id.value}`, query: route.query }) } } }
  finally { busy.value = false; freeze.value = false }
}
async function conflictServer() { if (!serverVersion.value) return; if (!await confirmAction('确认恢复服务器版本？当前本地内容将被替换，请先复制需要保留的内容。')) return; apply(serverVersion.value); clearBackup(); error.value = ''; notice.value = '已恢复服务器版本。' }
async function conflictLocal() {
  if (!serverVersion.value || !editable.value || !await confirmAction(article.value?.status === 'PUBLISHED' ? '确认用本地内容覆盖当前服务器版本并立即更新公开文章？' : '确认用本地内容覆盖当前服务器版本？其他标签页的修改可能被替换。')) return
  applying = true; form.value.revision = serverVersion.value.revision; applying = false; serverVersion.value = undefined; await save()
}
async function copyLocal() { try { await navigator.clipboard.writeText(JSON.stringify(form.value, null, 2)); copyFeedback.value = '本地全文已复制（包含原始代码）。' } catch { copyFeedback.value = '复制失败，请在下方文本框选择并复制。' } }
async function refreshConflict() { if (!id.value || currentUser.value?.id !== owner || authStatus.value !== 'authenticated') return; saver.pause(); try { const value = await getData<ManagedArticle>(`/account/articles/${id.value}`); if (currentUser.value?.id === owner && authStatus.value === 'authenticated') serverVersion.value = value } catch { notice.value = '服务器版本暂时无法读取，请重试。' } }
async function action(name: string) {
  if (!article.value || busy.value || serverVersion.value || authStatus.value !== 'authenticated' || currentUser.value?.id !== owner) return
  if (name === 'reject' && !reason.value.trim()) { error.value = '请填写驳回原因。'; return }
  if (dirty.value && !['submit', 'publish'].includes(name)) { error.value = '请先保存修改再执行状态操作。'; return }
  busy.value = true; freeze.value = true; error.value = ''
  try {
    if (dirty.value && !await saver.flush()) return
    const prompts: Record<string,string> = { submit: '确认提交最新保存的内容审核？待审期间不能编辑正文。', withdraw: '确认撤回尚未处理的投稿？', approve: '确认批准这份投稿并公开发布？', reject: '确认驳回此投稿并记录原因？', publish: '确认直接公开发布这篇文章？', archive: '确认下架文章？原始内容和审核记录会保留。' }
    if (!await confirmAction(prompts[name]!)) return
    const path = ['submit', 'withdraw'].includes(name) ? '/account' : '/admin'
    const a = await postData<ManagedArticle>(`${path}/articles/${article.value.id}/${name}`, { revision: article.value.revision, reason: reason.value }); apply(a); clearBackup(); reason.value = ''; notice.value = '操作完成。'
  } catch (e) { error.value = (e as Error).message; if (e instanceof ApiError && e.status === 409) await refreshConflict() } finally { busy.value = false; freeze.value = false; if (dirty.value) saver.edited() }
}
async function flags() { if (!article.value || busy.value || dirty.value || authStatus.value !== 'authenticated' || currentUser.value?.id !== owner) return; busy.value = true; error.value = ''; try { apply(await putData(`/admin/articles/${article.value.id}/flags`, { revision: article.value.revision, featured: article.value.featured, wide: article.value.wide })); notice.value = '推荐设置已保存。' } catch (e) { error.value = (e as Error).message; if (e instanceof ApiError && e.status === 409) await refreshConflict() } finally { busy.value = false } }
function moveSection(index: number, offset: number) { const target = index + offset; if (target < 0 || target >= form.value.sections.length) return; const moved = form.value.sections.splice(index, 1)[0]!; form.value.sections.splice(target, 0, moved) }
async function removeSection(index: number) { if (await confirmAction('删除当前章节及其中内容？')) form.value.sections.splice(index, 1) }
function addSection() { form.value.sections.push(newSection()) }
function insertTab(event: KeyboardEvent, section: ArticleSection) { const textarea = event.target as HTMLTextAreaElement; textarea.setRangeText('    ', textarea.selectionStart, textarea.selectionEnd, 'end'); section.code = textarea.value }
let removeLeaveGuard: (() => void) | undefined, leaving = false
async function leave() {
  if (!dirty.value) return true
  backup(); saver.pause()
  const choice = await chooseAction(backupSafe ? '修改尚未保存到服务器，已保留本机备份。请选择离开方式。' : '修改未保存，且本机备份不可用。直接离开会丢失当前输入。', [{ value: 'cancel', label: '取消' }, ...(editable.value && !serverVersion.value ? [{ value: 'save', label: '保存后离开' }] : []), { value: 'leave', label: backupSafe ? '保留本地后离开' : '确认直接离开' }])
  if (choice === 'cancel') { if (autoAllowed.value && saveState.value !== 'failed') { saver.reset(); saver.edited() } return false }
  if (choice === 'save') {
    if (article.value?.status === 'PUBLISHED' && !await confirmAction('保存后立即更新公开文章。确认保存后离开？')) return false
    busy.value = true
    try { if (!await saver.flush() || dirty.value || serverVersion.value) return false } finally { busy.value = false }
    if (id.value) clearBackup()
  }
  return true
}
function beforeUnload(e: BeforeUnloadEvent) { if (dirty.value && !leaving) { backup(); e.preventDefault(); e.returnValue = '' } }
async function navigation(event: MouseEvent) {
  if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || !dirty.value) return
  const link = event.target instanceof Element ? event.target.closest<HTMLAnchorElement>('a[href]') : null
  if (!link || link.target === '_blank' || link.hasAttribute('download')) return
  const destination = new URL(link.href, location.href)
  if (destination.origin !== location.origin || (destination.pathname === location.pathname && destination.search === location.search)) return
  event.preventDefault()
  if (await requestLeave()) { leaving = true; location.assign(destination.href) }
}
onBeforeRouteLeave(requestLeave)
onMounted(() => { void load(); removeLeaveGuard = registerLeaveGuard(leave); window.addEventListener('beforeunload', beforeUnload); document.addEventListener('click', navigation, true) })
onBeforeUnmount(() => { saver.dispose(); removeLeaveGuard?.(); window.removeEventListener('beforeunload', beforeUnload); document.removeEventListener('click', navigation, true) })
</script>
<template>
  <header class="account-heading"><div><RouterLink :to="returnTo">← 返回文章列表</RouterLink><h1>{{ article?.title || (id ? loading ? '正在加载文章…' : '文章不可访问' : '新建文章') }}</h1><p><span v-if="article" class="account-badge" :data-status="article.status">{{ statusNames[article.status] }}</span> {{ article ? `作者：${article.author}` : id ? '正在读取正文与审核记录。' : '准备好后再发布或投稿。' }}</p></div></header>
  <p class="account-feedback" :class="{ error }" role="status">{{ error || notice }} <button v-if="error && !serverVersion && saveState === 'failed'" class="account-text-button" :disabled="busy || !editable" @click="save">重试保存</button><button v-if="error && !serverVersion" class="account-text-button" :disabled="busy" @click="id ? refreshConflict() : load()">{{ id ? '查看服务器版本' : '重新加载' }}</button></p>
  <section v-if="serverVersion" class="account-card account-conflict" role="region" aria-label="版本冲突处理"><h2>版本冲突 · 自动保存已暂停</h2><p>本地版本 {{ form.revision }}，服务器版本 {{ serverVersion.revision }}。双方内容均保留，选择前不会覆盖服务器。</p><div class="account-row"><button class="account-button" @click="showServer = !showServer">{{ showServer ? '收起服务器版本' : '查看服务器版本' }}</button><button class="account-button" @click="copyLocal">复制本地全文</button><button class="account-button" :disabled="busy" @click="conflictServer">确认恢复服务器版本</button><button class="account-button danger" :disabled="busy || !editable || serverVersion.status === 'PENDING'" @click="conflictLocal">用本地内容覆盖服务器</button></div><p class="account-feedback" role="status">{{ copyFeedback }}</p><textarea v-if="copyFeedback.includes('失败')" :value="JSON.stringify(form, null, 2)" readonly rows="6" aria-label="可复制的本地全文"></textarea>
    <article v-if="showServer" class="account-preview"><h2>{{ serverVersion.title }}</h2><p>{{ serverVersion.summary }}</p><section v-for="(s,i) in serverVersion.sections" :key="i"><component :is="`h${s.level || 2}`">{{ s.heading }}</component><p v-for="(p,j) in s.paragraphs" :key="j">{{ p }}</p><ul><li v-for="(b,j) in s.bullets" :key="j">{{ b }}</li></ul><ArticleCodeBlock v-if="s.code" :code="s.code" :language="s.codeLanguage" :label="s.heading" /></section></article>
  </section>
  <div v-if="loading" class="account-card account-empty" aria-busy="true">正在加载编辑器…</div>
  <div v-else-if="id && !article" class="account-card account-empty"><h2>文章不可访问</h2><p>文章不存在、网络不可用或没有查看权限。</p><button class="account-button" @click="load">重试加载</button><RouterLink :to="returnTo">返回列表</RouterLink></div>
  <div v-else class="account-editor-grid">
    <section class="account-card"><div class="account-editor-toolbar"><button class="account-button" :aria-pressed="!preview" @click="preview = false">编辑</button><button class="account-button" :aria-pressed="preview" @click="preview = true">预览</button><span v-if="id" class="account-muted">文章编号 {{ id }}</span></div>
      <div class="account-save-feedback" :data-state="saveState" role="status" aria-live="polite"><strong>{{ ({ unsaved: '未保存', saving: '保存中…', saved: '已保存', failed: '保存失败 · 自动保存已暂停' })[saveState] }}</strong><span>{{ lastSavedAt ? `最近成功：${time(lastSavedAt)}` : article?.status === 'PUBLISHED' ? '公开文章需手动确认保存。' : article?.status === 'PENDING' ? '待审内容已锁定。' : '输入停止后自动保存草稿；未通过校验时保留本机备份。' }}</span><p v-if="validation">{{ validation }}</p></div>
      <p v-if="!editable" class="account-notice">{{ authStatus !== 'authenticated' ? '身份尚未确认或会话不可用，保存已暂停，输入与本地草稿仍保留。' : article?.status === 'PENDING' ? '待审核内容已锁定。作者可撤回后修改。' : '已发布或下架文章由管理员处理，当前仅可查看。' }}</p>
      <p v-if="article?.status === 'PUBLISHED' && admin" class="account-notice">公开文章不会自动保存。点击“保存并更新公开文章”确认后，修改立即对访客生效。</p>
      <form v-show="!preview" class="account-editor-form" @submit.prevent="save"><fieldset :disabled="!editable || freeze"><label>文章标题<input v-model="form.title" required maxlength="255" /></label><label>摘要<textarea v-model="form.summary" required maxlength="2000" rows="3"></textarea></label>
        <label>分类<select v-model="form.categoryId"><option v-for="category in options.categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
        <fieldset class="account-tag-picker"><legend>算法标签 · 已选 {{ form.tagIds.length }} / 8</legend><div class="account-selected-tags"><button v-for="tag in selectedTags" :key="tag.id" type="button" class="account-button" :aria-label="`移除标签 ${tag.name}`" @click="form.tagIds = form.tagIds.filter(id => id !== tag.id)">{{ tag.name }} ×</button><span v-if="!selectedTags.length" class="account-muted">尚未选择标签</span></div><label class="account-tag-search">搜索标签<input v-model="tagSearch" type="search" placeholder="输入标签名称" /></label><div class="account-tag-options"><label v-for="tag in visibleTags" :key="tag.id"><input v-model="form.tagIds" type="checkbox" :value="tag.id" :disabled="form.tagIds.length >= 8" />{{ tag.name }}</label><span v-if="!visibleTags.length" class="account-muted">没有可选的匹配标签。</span></div></fieldset>
        <section v-for="(section, index) in form.sections" :key="index" class="account-section-editor"><div class="account-row"><h2>章节 {{ index + 1 }}</h2><div class="account-section-actions"><button type="button" class="account-button" :disabled="index === 0" :aria-label="`上移章节 ${index + 1}`" @click="moveSection(index, -1)">上移</button><button type="button" class="account-button" :disabled="index === form.sections.length - 1" :aria-label="`下移章节 ${index + 1}`" @click="moveSection(index, 1)">下移</button><button type="button" class="account-text-button danger" @click="removeSection(index)">删除章节</button></div></div><label>章节标题<input v-model="section.heading" required maxlength="160" /></label><label>标题层级<select v-model="section.level"><option :value="2">H2 二级标题</option><option :value="3">H3 三级标题</option><option :value="4">H4 四级标题</option></select></label>
          <label v-for="(_, p) in section.paragraphs" :key="p">段落 {{ p + 1 }}<textarea v-model="section.paragraphs[p]" rows="4" maxlength="12000"></textarea><button type="button" class="account-text-button" @click="section.paragraphs.splice(p, 1)">删除此段落</button></label><button type="button" class="account-button" @click="section.paragraphs.push('')">添加段落</button>
          <label>列表项（每行一项）<textarea :value="section.bullets?.join('\n')" rows="3" @input="section.bullets = ($event.target as HTMLTextAreaElement).value.split('\n').filter(x => x.trim())"></textarea></label>
          <label>代码展示语言<select v-model="section.codeLanguage"><option v-for="language in codeLanguages" :key="language.value" :value="language.value">{{ language.label }}</option></select></label><p class="account-muted">仅影响文章展示，不代表 OJ 支持执行此语言。</p>
          <label>代码块（原始文本与换行保留）<textarea v-model="section.code" class="account-code-input" rows="9" spellcheck="false" maxlength="100000" @keydown.tab.prevent="insertTab($event, section)"></textarea></label>
        </section><button type="button" class="account-button" :disabled="form.sections.length >= 40" @click="addSection">添加章节</button>
      </fieldset></form>
      <article v-show="preview" class="account-preview"><span class="account-badge">文章预览</span><h1>{{ form.title || '未填写标题' }}</h1><p>{{ form.summary }}</p><section v-for="(section, index) in form.sections" :key="index"><component :is="`h${section.level || 2}`">{{ section.heading }}</component><p v-for="(paragraph, p) in section.paragraphs" :key="p">{{ paragraph }}</p><ul v-if="section.bullets?.length"><li v-for="(bullet, b) in section.bullets" :key="b">{{ bullet }}</li></ul><ArticleCodeBlock v-if="section.code" :code="section.code" :language="section.codeLanguage" :label="section.heading" /></section></article>
    </section>
    <aside class="account-editor-aside"><section class="account-card"><h2>文章操作</h2><button v-if="editable" class="account-button primary" :disabled="busy || !!serverVersion" @click="save">{{ busy ? '处理中…' : article?.status === 'PUBLISHED' ? '保存并更新公开文章' : '保存草稿' }}</button><button v-if="article && ['DRAFT', 'REJECTED'].includes(article.status) && !admin" class="account-button" :disabled="!editable || busy || !!serverVersion" @click="action('submit')">保存最新内容并提交审核</button><button v-if="article?.status === 'PENDING' && article.authorId === currentUser?.id" class="account-button" :disabled="authStatus !== 'authenticated' || currentUser?.id !== owner || busy" @click="action('withdraw')">撤回投稿</button>
      <template v-if="admin && article"><button v-if="['DRAFT', 'REJECTED', 'ARCHIVED'].includes(article.status)" class="account-button primary" :disabled="busy || !!serverVersion" @click="action('publish')">直接发布</button><template v-if="article.status === 'PENDING'"><button class="account-button primary" :disabled="busy" @click="action('approve')">批准并发布</button><label>驳回原因<textarea v-model="reason" rows="4" maxlength="2000" :disabled="busy"></textarea></label><button class="account-button danger" :disabled="busy" @click="action('reject')">驳回投稿</button></template><button v-if="article.status === 'PUBLISHED'" class="account-button danger" :disabled="busy || dirty" @click="action('archive')">下架文章</button><fieldset :disabled="busy || dirty || !!serverVersion"><legend>展示设置</legend><label class="account-check"><input v-model="article.featured" type="checkbox" />推荐文章</label><label class="account-check"><input v-model="article.wide" type="checkbox" />横向推荐卡片</label><button class="account-button" @click="flags">保存展示设置</button></fieldset></template>
      <a v-if="article?.status === 'PUBLISHED'" class="account-button" :href="`./article.html?id=${article.id}`">查看公开页面</a><p class="account-muted">投稿时间：{{ time(article?.submittedAt) }}<br/>更新时间：{{ time(article?.updatedAt) }}<br/>时间按 Asia/Shanghai 展示。</p>
    </section><section v-if="article?.reviews.length" class="account-card"><h2>审核记录</h2><article v-for="review in article.reviews" :key="review.round" class="account-review"><strong>{{ ({ APPROVED: '审核通过', REJECTED: '已驳回', ARCHIVED: '已下架', PUBLISHED: '管理员直接发布' } as Record<string, string>)[review.decision] }}</strong><p v-if="review.reason">{{ review.reason }}</p><small>{{ review.reviewer }} · {{ time(review.reviewedAt) }}</small></article></section></aside>
  </div>
</template>



