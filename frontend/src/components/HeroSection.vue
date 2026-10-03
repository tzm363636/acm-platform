<script setup lang="ts">
import CodeEditorMock from './CodeEditorMock.vue'
</script>

<template>
  <section id="home" class="hero-section section-anchor">
    <div class="container hero-grid">
      <div class="hero-copy fade-up">
        <span class="hero-badge"><i aria-hidden="true"></i>ACM · CODE · SHARE</span>
        <h1>让代码有答案，<span>也让经验被看见</span></h1>
        <p>一个面向程序设计学习者的个人技术平台。通过 ACM 在线评测验证能力，也通过文章与代码分享复盘并沉淀经验。</p>
        <div class="hero-actions">
          <a class="button button-primary" href="#oj">进入在线评测 <span aria-hidden="true">↗</span></a>
          <a class="button button-secondary" href="#sharing">浏览代码文章 <span aria-hidden="true">→</span></a>
        </div>
      </div>
      <div class="hero-visual fade-up"><CodeEditorMock /></div>
      <a class="scroll-cue" href="#platform">向下了解<span aria-hidden="true"></span></a>
    </div>
  </section>
</template>
