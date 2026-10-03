<script setup lang="ts">
import { ref } from 'vue'

const showResult = ref(false)
</script>

<template>
  <div class="code-editor" aria-label="C++ 代码展示">
    <div class="editor-titlebar">
      <div class="window-dots" aria-hidden="true"><i></i><i></i><i></i></div>
      <span>main.cpp · C++17</span>
    </div>
    <div class="editor-body">
      <div class="line-numbers" aria-hidden="true">1<br>2<br>3<br>4<br>5<br>6<br>7<br>8</div>
      <pre class="code-lines"><code><span class="token-violet">#include</span> <span class="token-cyan">&lt;bits/stdc++.h&gt;</span>

<span class="token-violet">using namespace</span> std;
<span class="token-cyan">int</span> main() {
  ios::sync_with_stdio(<span class="token-gold">false</span>);
  cout &lt;&lt; <span class="token-mint">"Hello, ACM!"</span>;
  <span class="token-violet">return</span> <span class="token-gold">0</span>;
}</code></pre>
    </div>
    <div class="editor-status">
      <div class="judge-metrics" aria-live="polite">
        <span><small>状态</small><strong :class="showResult ? 'accepted' : ''">{{ showResult ? 'Accepted' : 'Ready' }}</strong></span>
        <span><small>运行时间</small><strong>{{ showResult ? '31 ms' : '—' }}</strong></span>
        <span><small>内存</small><strong>{{ showResult ? '2.1 MB' : '—' }}</strong></span>
      </div>
      <button type="button" class="run-button" @click="showResult = !showResult">{{ showResult ? '重置演示' : '运行演示' }}</button>
    </div>
    <p v-if="showResult" class="demo-note">这是首页视觉演示，尚未接入真实 OJ 判题。</p>
  </div>
</template>
