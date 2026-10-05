import { computed, ref } from 'vue'
import { practiceStats } from './api'
import { ojApi } from './adapter'
import { databaseMode } from '../api/database'
import { databaseApi } from './databaseApi'
import type { Submission } from './types'

export const records = ref<Submission[]>(ojApi.peek())
export const storageNotice = ref(ojApi.notice)
export const demoIdentity = ref(false)
try { demoIdentity.value = localStorage.getItem('acm-oj-demo-identity') === 'on' } catch { /* Optional preference. */ }
export function enableDemoIdentity() {
  demoIdentity.value = true
  try { localStorage.setItem('acm-oj-demo-identity', 'on') } catch { /* Current session still works. */ }
}
export function disableDemoIdentity() {
  demoIdentity.value = false
  try { localStorage.setItem('acm-oj-demo-identity', 'off') } catch { /* Current session still works. */ }
}
export function syncRecords() { records.value = ojApi.peek(); storageNotice.value = ojApi.notice; if(databaseMode) void refreshProgress() }
const databaseProgress = ref({passed:0,total:0,recent:0,review:0 as number|null})
export const progressError = ref('')
let progressRequest = 0
export async function refreshProgress() {
  const request = ++progressRequest
  try {
    const data = await databaseApi.practice()
    if (request === progressRequest) { databaseProgress.value = data; progressError.value = '' }
  } catch {
    if (request === progressRequest) progressError.value = '练习统计暂时无法刷新，请重试。'
  }
}
export const progressStats = computed(() => databaseMode ? databaseProgress.value : practiceStats(records.value))
export function listReturnUrl() {
  try { return sessionStorage.getItem('acm-oj-list-url') || '/problems' } catch { return '/problems' }
}
export function rememberListUrl(url: string) {
  try { sessionStorage.setItem('acm-oj-list-url', url) } catch { /* Query strings still preserve current filters. */ }
}
export function submissionsReturnUrl() {
  try { return sessionStorage.getItem('acm-oj-submissions-url') || '/submissions' } catch { return '/submissions' }
}
export function rememberSubmissionsUrl(url: string) {
  try { sessionStorage.setItem('acm-oj-submissions-url', url) } catch { /* Optional navigation memory. */ }
}
export const formatTime = (value?: string) => value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'medium',timeZone:'Asia/Shanghai' }).format(new Date(value)) : '—'
