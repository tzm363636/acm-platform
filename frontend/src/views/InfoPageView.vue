<script setup lang="ts">
import Navbar from '../components/Navbar.vue'
import AboutSection from '../components/AboutSection.vue'
import MilestoneSection from '../components/MilestoneSection.vue'

defineProps<{ page: 'oj' | 'about' | 'milestone' }>()
</script>

<template>
  <Navbar :active-page="page" />
  <main class="page-flow info-page">
    <section v-if="page === 'oj'" class="info-oj" aria-labelledby="oj-title">
      <div class="info-oj-card">
        <span class="eyebrow">在线评测</span>
        <h1 id="oj-title">OJ</h1>
        <p>具体内容后续添加。</p>
      </div>
    </section>
    <AboutSection v-else-if="page === 'about'" />
    <MilestoneSection v-else />
  </main>
</template>
