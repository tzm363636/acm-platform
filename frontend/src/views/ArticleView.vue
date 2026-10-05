<script setup lang="ts">
import { computed, ref } from 'vue'
import Navbar from '../components/Navbar.vue'
import ArticleCodeBlock from '../components/ArticleCodeBlock.vue'
import { articles } from '../data/articles'
import { useArticleReading } from '../composables/useArticleReading'

const readingBody = ref<HTMLElement | null>(null)
const readingToc = ref<HTMLElement | null>(null)
const { headings, activeHeading, progress, tocExpanded, headerHeight, scrollOffset, goToHeading } = useArticleReading(readingBody, readingToc)

const params = new URLSearchParams(window.location.search)
const id = Number(params.get('id'))
const fromQuery = new URLSearchParams(params.get('from') || '').toString()
const returnUrl = `./code-sharing.html${fromQuery ? `?${fromQuery}` : ''}`
const article = computed(() => articles.find((item) => item.id === id))
document.title = article.value ? `${article.value.title} · ACM Code Share` : '文章未找到 · ACM Code Share'
</script>

<template>
  <Navbar active-page="sharing" />
  <div v-if="article" class="reading-progress" role="progressbar" aria-label="文章阅读进度" :aria-valuenow="Math.round(progress)" aria-valuemin="0" aria-valuemax="100"><span :style="{ transform: `scaleX(${progress / 100})` }"></span></div>
  <main class="reading-page" :style="{ '--reading-header-height': `${headerHeight}px`, '--reading-scroll-offset': `${scrollOffset}px` }">
    <div class="reading-container" :class="{ 'reading-container-with-toc': headings.length }">
      <nav class="reading-breadcrumb" aria-label="面包屑导航"><a :href="returnUrl">代码分享</a><span aria-hidden="true">/</span><span>{{ article?.category || '文章' }}</span></nav>
      <div v-if="article" class="reading-layout" :class="{ 'reading-layout-with-toc': headings.length }">
      <article class="reading-article">
        <header class="reading-header">
          <span class="reading-category">{{ article.category }}</span>
          <h1>{{ article.title }}</h1>
          <p>{{ article.summary }}</p>
          <div class="reading-tags"><span v-for="tag in article.tags" :key="tag">{{ tag }}</span></div>
          <a class="reading-author" href="./author.html">作者：田振民 <span aria-hidden="true">→</span></a>
        </header>
        <div ref="readingBody" class="reading-body">
          <section v-for="(section, index) in article.sections" :id="`section-${index + 1}`" :key="index">
            <component :is="`h${section.level || 2}`">{{ section.heading }}</component>
            <p v-for="paragraph in section.paragraphs" :key="paragraph">{{ paragraph }}</p>
            <ul v-if="section.bullets"><li v-for="bullet in section.bullets" :key="bullet">{{ bullet }}</li></ul>
            <ArticleCodeBlock v-if="section.code" :code="section.code" :label="section.heading" />
          </section>
        </div>
        <footer class="reading-footer"><a :href="returnUrl">← 返回代码分享</a><a href="./author.html">了解作者 →</a></footer>
      </article>
      <aside v-if="headings.length" ref="readingToc" class="reading-toc">
        <h2 class="reading-toc-title">文章目录 <span>{{ Math.round(progress) }}%</span></h2>
        <button type="button" class="reading-toc-toggle" :aria-expanded="tocExpanded" aria-controls="article-toc-links" @click="tocExpanded = !tocExpanded"><span>文章目录 <small>{{ Math.round(progress) }}%</small></span><span aria-hidden="true">{{ tocExpanded ? '收起 −' : '展开 +' }}</span></button>
        <nav id="article-toc-links" aria-label="文章目录" :class="{ 'reading-toc-expanded': tocExpanded }">
          <ol><li v-for="heading in headings" :key="heading.id" :style="{ '--toc-level': heading.level - 2 }"><a :href="`#${heading.id}`" :class="{ active: activeHeading === heading.id }" :aria-current="activeHeading === heading.id ? 'location' : undefined" @click.prevent="goToHeading(heading.id)">{{ heading.text }}</a></li></ol>
        </nav>
      </aside>
      </div>
      <div v-else class="reading-empty"><h1>没有找到这篇文章</h1><p>链接可能有误，返回代码分享页面查看现有文章。</p><a :href="returnUrl">返回代码分享 →</a></div>
    </div>
  </main>
</template>
