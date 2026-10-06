import type { ArticleSection } from '../data/articles'
import { codeLanguage } from '../components/codeLanguages'
/** MySQL JSON changes key order. Compare content, never serialized storage order or revision. */
export function contentSignature(f: { title: string; summary: string; categoryId: number; tagIds: number[]; sections: ArticleSection[] }) {
  return JSON.stringify({ title: f.title.trim(), summary: f.summary.trim(), categoryId: f.categoryId, tagIds: [...f.tagIds].sort((a,b) => a-b), sections: f.sections.map(s => ({ heading: s.heading, level: s.level || 2, paragraphs: s.paragraphs, bullets: s.bullets || [], code: s.code || '', codeLanguage: codeLanguage(s.codeLanguage).value })) })
}
