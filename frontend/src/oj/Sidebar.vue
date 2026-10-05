<script setup lang="ts">
import { computed } from 'vue'
import { articles, articleUrl } from '../data/articles'
import { demoIdentity, enableDemoIdentity, progressStats } from './store'
const props = defineProps<{ articleIds?: number[] }>()
const recommended = computed(() => props.articleIds ? articles.filter(a => props.articleIds!.includes(a.id)) : articles.filter(a => [1, 2, 3, 6].includes(a.id)))
</script>
<template>
  <aside class="oj-sidebar">
    <section class="oj-card oj-progress-card"><h2>练习进度 <small>本机演示</small></h2><div class="oj-progress-line"><div class="oj-ring" :style="{ '--progress': `${progressStats.passed / progressStats.total * 100}%` }"><span>✓</span></div><div><strong>{{ progressStats.passed }} <small>/ {{ progressStats.total }}</small></strong><p>不同题目的演示 AC 数</p></div></div><div class="oj-progress-stats"><span>最近 7 天通过 <b>{{ progressStats.recent }}</b></span><span>待复盘 <b>{{ progressStats.review }}</b></span></div><p class="oj-muted">来源：本机演示提交，AC 按题目去重；待复盘为尝试但未通过的题目。</p><button v-if="!demoIdentity" class="oj-button small" @click="enableDemoIdentity">体验个人状态（演示）</button></section>
    <section class="oj-card"><h2>▤ 相关文章推荐</h2><a v-for="article in recommended" :key="article.id" class="oj-article-link" :href="articleUrl(article.id)"><span>▧</span><div><strong>{{ article.title }}</strong><p>{{ article.summary }}</p></div><span>›</span></a><p v-if="!recommended.length" class="oj-muted">当前题目暂无对应文章。</p></section>
    <section class="oj-card oj-tip"><h2>☀ 练习提示</h2><p>浏览题目、编辑代码、查看样例。演示场景只验证操作流程；真实代码正确性需要接入判题服务。</p></section>
  </aside>
</template>
