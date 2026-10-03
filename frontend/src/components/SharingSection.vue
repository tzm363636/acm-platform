<template>
  <section id="sharing" class="content-section sharing-section section-anchor">
    <div class="container sharing-grid">
      <div>
        <span class="eyebrow">CONTENT SHARING</span>
        <h2>把一次解题，整理成一篇有用的文章</h2>
        <p class="section-description">提交结果只是终点的一半。把思路、边界和代码写清楚，才能让一次练习真正成为可复用的经验。</p>
        <ol class="sharing-steps">
          <li><span>01</span><div><strong>记录思路</strong><p>先解释为什么这样做，再展示实现细节。</p></div></li>
          <li><span>02</span><div><strong>分享代码</strong><p>用清晰高亮的代码块保留关键实现。</p></div></li>
          <li><span>03</span><div><strong>关联题目</strong><p>文章与 OJ 题目互相连接，阅读后可以立即练习。</p></div></li>
        </ol>
      </div>
      <div class="blog-window" aria-label="文章形式预览">
        <div class="blog-window-top"><div class="window-dots" aria-hidden="true"><i></i><i></i><i></i></div><span>代码分享 · 文章预览</span></div>
        <div class="blog-window-body">
          <small>算法题解 · 二分查找</small>
          <h3>如何避免二分答案中的边界错误</h3>
          <p>从循环条件、左右端点和返回值开始，逐步解释最容易出错的三个位置……</p>
          <div class="blog-minis"><div><strong>C++ 核心代码</strong><code>while (left &lt;= right) { ... }</code></div><div><strong>关联题目</strong><span>ACM-1024 · 示例</span></div></div>
          <p class="preview-note">示例内容 · 文章功能尚未上线</p>
        </div>
      </div>
    </div>
  </section>
</template>
