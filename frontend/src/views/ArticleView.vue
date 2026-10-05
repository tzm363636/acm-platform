<script setup lang="ts">
import { computed } from 'vue'
import Navbar from '../components/Navbar.vue'
import ArticleCodeBlock from '../components/ArticleCodeBlock.vue'
import { articles } from '../data/articles'

const params = new URLSearchParams(window.location.search)
const id = Number(params.get('id'))
const fromQuery = new URLSearchParams(params.get('from') || '').toString()
const returnUrl = `./code-sharing.html${fromQuery ? `?${fromQuery}` : ''}`
const article = computed(() => articles.find((item) => item.id === id))
document.title = article.value ? `${article.value.title} · ACM Code Share` : '文章未找到 · ACM Code Share'
</script>

<template>
  <Navbar active-page="sharing" />
  <main class="reading-page">
    <div class="reading-container">
      <nav class="reading-breadcrumb" aria-label="面包屑导航"><a :href="returnUrl">代码分享</a><span aria-hidden="true">/</span><span>{{ article?.category || '文章' }}</span></nav>
      <article v-if="article" class="reading-article">
        <header class="reading-header">
          <span class="reading-category">{{ article.category }}</span>
          <h1>{{ article.title }}</h1>
          <p>{{ article.summary }}</p>
          <div class="reading-tags"><span v-for="tag in article.tags" :key="tag">{{ tag }}</span></div>
          <a class="reading-author" href="./author.html">作者：田振民 <span aria-hidden="true">→</span></a>
          <nav class="reading-toc" aria-label="本文目录">
            <h2>本文目录</h2>
            <ol><li v-for="(section, index) in article.sections" :key="section.heading"><a :href="`#section-${index + 1}`">{{ section.heading }}</a></li></ol>
          </nav>
        </header>
        <div class="reading-body">
          <section v-for="(section, index) in article.sections" :id="`section-${index + 1}`" :key="section.heading">
            <h2>{{ section.heading }}</h2>
            <p v-for="paragraph in section.paragraphs" :key="paragraph">{{ paragraph }}</p>
            <ul v-if="section.bullets"><li v-for="bullet in section.bullets" :key="bullet">{{ bullet }}</li></ul>
            <ArticleCodeBlock v-if="section.code" :code="section.code" :label="section.heading" />
          </section>
        </div>
        <footer class="reading-footer"><a :href="returnUrl">← 返回代码分享</a><a href="./author.html">了解作者 →</a></footer>
      </article>
      <div v-else class="reading-empty"><h1>没有找到这篇文章</h1><p>链接可能有误，返回代码分享页面查看现有文章。</p><a :href="returnUrl">返回代码分享 →</a></div>
    </div>
  </main>
</template>
