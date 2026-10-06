import { ref } from 'vue'
import { ApiError, getData, postData } from '../api/database'
import { clearCsrf } from '../api/client'
export interface User { id: number; username: string; displayName: string; role: 'USER' | 'ADMIN'; createdAt: string }
export const currentUser = ref<User | null>(null)
export const authReady = ref(false)
export const sessionExpired = ref(false)
export const authIssue = ref('')
let request: Promise<void> | undefined
let generation = 0
export function loadUser(force = false) {
  if (force && authReady.value) request = undefined
  if (!request) { const mine = ++generation; request = getData<{ user: User | null }>('/auth/me').then(r => { if (mine !== generation) return; if (!r.user && currentUser.value) expireSession(); else currentUser.value = r.user; authReady.value = true; authIssue.value = '' }).catch(e => { if (mine !== generation) return; if (e instanceof ApiError && e.status === 401) expireSession(); authReady.value = true; authIssue.value = e.message; request = undefined }) }
  return request
}
export async function login(username: string, password: string) { const r = await postData<{ user: User }>('/auth/login', { username, password }); generation++; request = undefined; clearCsrf(); currentUser.value = r.user; authReady.value = true; authIssue.value = ''; sessionExpired.value = false; notifyIdentityChange() }
export async function logout() { await postData('/auth/logout', {}); generation++; clearCsrf(); currentUser.value = null; request = undefined; sessionExpired.value = false; notifyIdentityChange() }
export function expireSession() { generation++; currentUser.value = null; sessionExpired.value = true; request = undefined; clearCsrf() }
window.addEventListener('acm-session-expired', expireSession)
// Only an invalidation signal is stored; identity and credentials are never persisted here.
export function notifyIdentityChange() { try { localStorage.setItem('acm-account-refresh', crypto.randomUUID()) } catch {} }
window.addEventListener('storage', e => { if (e.key === 'acm-account-refresh') void loadUser(true) })
document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible' && authReady.value) void loadUser(true) })
export function safeReturn(value: string | null) {
  if (!value || /[\\\x00-\x1f]/.test(value)) return './code-sharing.html'
  try { const url = new URL(value, location.href); const base = location.pathname.slice(0, location.pathname.lastIndexOf('/') + 1)
    if (url.origin !== location.origin || ![base, ...['index', 'code-sharing', 'article', 'author', 'oj', 'about', 'milestone', 'account'].map(x => `${base}${x}.html`)].includes(url.pathname)) return './code-sharing.html'
    if (url.pathname.endsWith('/account.html') && /^#\/(login|register)/.test(url.hash)) return './account.html#/articles'
    return url.pathname + url.search + url.hash
  } catch { return './code-sharing.html' }
}
export function loginUrl() { const source = new URL(location.href); if (source.pathname.endsWith('/account.html')) source.searchParams.delete('return'); return `./account.html?return=${encodeURIComponent(source.pathname + source.search + source.hash)}#/login` }

