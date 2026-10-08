import { reactive, shallowRef } from 'vue'
import type { ArticleSummary } from '../data/articles'
import type { ServerPage } from '../api/database'

export type ArticleSort = 'recommended' | 'published' | 'updated'
export interface SharingQuery { q: string; category: string; tag: string; sort: ArticleSort; page: number; size: number; more: boolean; allTags: boolean }
export interface ArticleOptions { categories: string[]; tags: string[]; publishedTotal: number; categoryCount: number }
export type ArticleFeed = ServerPage<ArticleSummary> & { options: ArticleOptions }
export function readSharingQuery(search: string): SharingQuery {
  const p = new URLSearchParams(search), positive = (key: string, fallback: number) => {
    const value = Number(p.get(key)); return Number.isSafeInteger(value) && value > 0 && value <= 1_000_000 ? value : fallback
  }
  const page = positive('page', 1), size = positive('size', 6)
  return { q: (p.get('q') || '').trim(), category: p.get('category') === '全部' ? '' : p.get('category') || '', tag: p.get('tag') === 'DP' ? '动态规划' : p.get('tag') || '', sort: ['published', 'updated'].includes(p.get('sort') || '') ? p.get('sort') as ArticleSort : 'recommended', page, size: [6,12,24].includes(size) ? size : 6, more: p.get('more') === '1' || page > 1, allTags: p.get('tags') === 'all' }
}
export function sharingSearch(q: SharingQuery) {
  const p = new URLSearchParams()
  if (q.category) p.set('category', q.category)
  if (q.tag) p.set('tag', q.tag)
  if (q.q) p.set('q', q.q)
  if (q.more) p.set('more', '1')
  if (q.allTags) p.set('tags', 'all')
  if (q.sort !== 'recommended') p.set('sort', q.sort)
  if (q.page > 1) p.set('page', String(q.page))
  if (q.size !== 6) p.set('size', String(q.size))
  return p.toString()
}
export function articleTime(value?: string | null) {
  if (!value || !Number.isFinite(Date.parse(value))) return '时间未记录'
  return new Intl.DateTimeFormat('zh-CN', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value))
}

// Only retain the last successful public page in component memory. Never persist private data.
export function createPublicFeed(fetcher: (query: SharingQuery, signal: AbortSignal) => Promise<ArticleFeed>) {
  const data = shallowRef<ArticleFeed | null>(null)
  const state = reactive({ loading: false, error: '', loadedKey: '', requestedKey: '' })
  let version = 0, controller: AbortController | undefined
  async function load(query: SharingQuery, beforeCommit?: () => void) {
    const snapshot = { ...query }, key = sharingSearch(snapshot), mine = ++version
    controller?.abort(); controller = new AbortController()
    state.requestedKey = key; state.loading = true; state.error = ''
    try {
      const result = await fetcher(snapshot, controller.signal)
      if (mine !== version) return false
      if (!Array.isArray(result?.items) || !Array.isArray(result.options?.categories) || !Array.isArray(result.options?.tags)
        || !Number.isSafeInteger(result.page) || !Number.isSafeInteger(result.total)
        || !Number.isSafeInteger(result.options.publishedTotal) || !Number.isSafeInteger(result.options.categoryCount)) {
        throw new Error('文章数据格式不兼容，请确认后端已更新后重试。')
      }
      beforeCommit?.(); data.value = result
      state.loadedKey = sharingSearch({ ...snapshot, page: result.page })
      return true
    } catch (error) {
      if (mine === version) state.error = error instanceof Error ? error.message : '文章加载失败，请重试。'
      return false
    } finally { if (mine === version) state.loading = false }
  }
  function dispose() { version++; controller?.abort() }
  return { data, state, load, dispose }
}
