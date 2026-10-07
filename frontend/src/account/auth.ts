import { computed, ref } from 'vue'
import { ApiError, getData, postData } from '../api/database'
import { clearCsrf } from '../api/client'
import { advanceIdentityEpoch, identityEpoch } from './identityEpoch'
export interface User { id: number; username: string; displayName: string; role: 'USER' | 'ADMIN'; createdAt: string }
export const currentUser = ref<User | null>(null)
export type AuthStatus = 'checking' | 'authenticated' | 'anonymous' | 'expired' | 'unavailable'
export const authStatus = ref<AuthStatus>('checking')
export const authReady = computed(() => authStatus.value !== 'checking')
export const sessionExpired = computed(() => authStatus.value === 'expired')
export const authChecking = ref(true)
export const authBusy = ref(false)
export const authIssue = ref('')
let request: Promise<void> | undefined
let refreshQueued = false
export function loadUser(force = false): Promise<void> {
  if (authBusy.value) { refreshQueued = true; return Promise.resolve() }
  if (request) return request
  if (!force && authReady.value) return Promise.resolve()
  const mine = identityEpoch()
  authChecking.value = true
  const pending = getData<{ user: User | null }>('/auth/me').then(r => {
    if (mine !== identityEpoch()) return
    const previous = currentUser.value
    if (previous?.id !== r.user?.id) { advanceIdentityEpoch(); clearCsrf() }
    currentUser.value = r.user
    authStatus.value = r.user ? 'authenticated' : previous || sessionExpired.value ? 'expired' : 'anonymous'
    authIssue.value = ''
  }).catch(e => {
    if (mine !== identityEpoch()) return
    if (e instanceof ApiError && e.status === 401) expireSession()
    else { authStatus.value = 'unavailable'; authIssue.value = (e as Error).message }
  }).finally(() => {
    if (request === pending) { request = undefined; authChecking.value = false }
    if (refreshQueued && !authBusy.value) { refreshQueued = false; return loadUser(true) }
  })
  request = pending
  return pending
}
async function mutateIdentity(path: '/auth/login' | '/auth/logout', body: unknown) {
  if (authBusy.value) throw new ApiError('正在处理账户操作，请稍候。')
  authBusy.value = true
  advanceIdentityEpoch()
  try {
    const result = await postData<{ user?: User }>(path, body)
    advanceIdentityEpoch(); clearCsrf()
    currentUser.value = result.user || null
    authStatus.value = result.user ? 'authenticated' : 'anonymous'
    authIssue.value = ''
    notifyIdentityChange()
    return result.user
  } catch (e) {
    if (!(e instanceof ApiError) || ![400, 401, 403, 429].includes(e.status || 0)) {
      authStatus.value = 'unavailable'; authIssue.value = (e as Error).message
    }
    throw e
  } finally {
    authBusy.value = false
    if (refreshQueued) { refreshQueued = false; await loadUser(true) }
  }
}
export async function login(username: string, password: string) {
  const user = await mutateIdentity('/auth/login', { username, password })
  if (authStatus.value !== 'authenticated' || currentUser.value?.id !== user?.id) throw new ApiError('账户状态发生变化，请重新确认登录。输入已保留。')
}
export async function logout() { await mutateIdentity('/auth/logout', {}) }
export function expireSession() { advanceIdentityEpoch(); currentUser.value = null; authStatus.value = 'expired'; authIssue.value = ''; clearCsrf() }
export function updateCurrentUser(user: User) {
  if (authStatus.value !== 'authenticated' || currentUser.value?.id !== user.id) return false
  advanceIdentityEpoch(); currentUser.value = user; notifyIdentityChange(); return true
}
window.addEventListener('acm-session-expired', expireSession)
// Only an invalidation signal is stored; identity and credentials are never persisted here.
export function notifyIdentityChange() { try { localStorage.setItem('acm-account-refresh', crypto.randomUUID()) } catch {} }
function identityChanged() {
  advanceIdentityEpoch(); clearCsrf(); authStatus.value = 'checking'
  if (request || authBusy.value) refreshQueued = true
  else void loadUser(true)
}
function storageChanged(e: StorageEvent) { if (e.key === 'acm-account-refresh') identityChanged() }
window.addEventListener('storage', storageChanged)
let lastFocusCheck = 0
function checkOnFocus() {
  if (document.visibilityState !== 'visible' || Date.now() - lastFocusCheck < 1000) return
  lastFocusCheck = Date.now(); void loadUser(true)
}
document.addEventListener('visibilitychange', checkOnFocus)
window.addEventListener('focus', checkOnFocus)
function restoredPage(e: PageTransitionEvent) { if (e.persisted) identityChanged() }
window.addEventListener('pageshow', restoredPage)
if (import.meta.hot) import.meta.hot.dispose(() => {
  window.removeEventListener('acm-session-expired', expireSession)
  window.removeEventListener('storage', storageChanged)
  window.removeEventListener('focus', checkOnFocus)
  window.removeEventListener('pageshow', restoredPage)
  document.removeEventListener('visibilitychange', checkOnFocus)
})
export function safeReturn(value: string | null) {
  if (!value || /[\\\x00-\x1f]/.test(value)) return './code-sharing.html'
  try { const url = new URL(value, location.href); const base = location.pathname.slice(0, location.pathname.lastIndexOf('/') + 1)
    if (url.origin !== location.origin || ![base, ...['index', 'code-sharing', 'article', 'author', 'oj', 'about', 'milestone', 'account'].map(x => `${base}${x}.html`)].includes(url.pathname)) return './code-sharing.html'
    if (url.pathname.endsWith('/account.html') && /^#\/(login|register)/.test(url.hash)) return './account.html#/articles'
    return url.pathname + url.search + url.hash
  } catch { return './code-sharing.html' }
}
export function loginUrl() { const source = new URL(location.href); if (source.pathname.endsWith('/account.html')) source.searchParams.delete('return'); return `./account.html?return=${encodeURIComponent(source.pathname + source.search + source.hash)}#/login` }

