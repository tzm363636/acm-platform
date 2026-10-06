import type { ArticleSection } from '../data/articles'
export type ArticleStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED' | 'ARCHIVED'
export const statusNames: Record<ArticleStatus, string> = { DRAFT: '草稿', PENDING: '待审核', PUBLISHED: '已发布', REJECTED: '已驳回', ARCHIVED: '已下架' }
export interface ArticleSummary { id: number; title: string; summary: string; category: string; status: ArticleStatus; author: string; featured: boolean; submittedAt: string | null; updatedAt: string; publishedAt: string | null; rejectionReason: string | null }
export interface ManagedArticle { id: number; title: string; summary: string; category: string; categoryId: number; tagIds: number[]; sections: ArticleSection[]; status: ArticleStatus; revision: number; author: string; authorId: number; featured: boolean; wide: boolean; submittedAt: string | null; updatedAt: string; publishedAt: string | null; reviews: { decision: string; reason: string; reviewer: string; reviewedAt: string; round: number }[] }
export interface Options { categories: { id: number; name: string }[]; tags: { id: number; name: string }[] }
export function time(value?: string | null) { return value ? new Date(value).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false }) : '—' }
