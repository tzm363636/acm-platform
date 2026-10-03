<script setup lang="ts">
import Navbar from '../components/Navbar.vue'
import HeroSection from '../components/HeroSection.vue'
import PlatformSection from '../components/PlatformSection.vue'
import SharingSection from '../components/SharingSection.vue'
import AboutSection from '../components/AboutSection.vue'
import MilestoneSection from '../components/MilestoneSection.vue'
</script>

<template>
  <a class="skip-link" href="#main">跳转到主要内容</a>
  <Navbar />
  <main id="main" class="page-flow">
    <HeroSection />
    <PlatformSection />
    <SharingSection />
    <AboutSection />
    <MilestoneSection />
  </main>
  <footer class="site-footer"><div class="container footer-inner"><strong>ACM Code Share</strong><span>ACM Online Judge · Code Sharing</span><span>© 2026 · 持续构建中</span><a href="#home">返回顶部 ↑</a></div></footer>
</template>
