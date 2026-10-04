<script setup lang="ts">
import Navbar from '../components/Navbar.vue'

const directions = ['算法与数据结构', 'ACM / ICPC 竞赛', '计算机基础', '软件工程', '后端与 Web 开发', '人工智能探索']
const journey = [
  { number: '01', title: '从职业高中出发', text: '最初对计算机的认识从编程和软件使用开始。进入计算机应用技术专业后，逐渐系统学习程序设计、数据结构与软件开发。' },
  { number: '02', title: '遇见算法竞赛', text: '在一次次刷题、调试和分析复杂度的过程中，开始体会到“会写代码”和“会解决问题”之间的差别，也逐渐喜欢上算法。' },
  { number: '03', title: '在失败里继续训练', text: 'WA、TLE、RE 和未达到预期的比赛成绩都是训练的一部分。和队友一起补题、复盘，让错误逐渐变成可以积累的经验。' },
  { number: '04', title: '走向全国赛场', text: '先后参加 CCPC、蓝桥杯、团体程序设计天梯赛、码蹄杯等赛事，获得多项国家级与省级竞赛荣誉。' },
  { number: '05', title: '从参赛者到 ACM 社社长', text: '参与校队训练、比赛组织和新成员培养，也开始思考怎样把算法知识讲清楚、怎样让更多人获得合适的练习资料。' },
  { number: '06', title: '专升本，继续学习软件工程', text: '从职高、专科走到本科，开始更系统地学习操作系统、计算机组成原理、计算机网络、数据库与软件工程。' },
  { number: '07', title: '构建自己的技术项目', text: '尝试把 ACM 训练、在线判题、代码分享和个人技术网站连接起来，从解一道题延伸到思考一个系统如何服务真正的用户。' },
  { number: '08', title: '现在，继续向前', text: '仍在学习，也仍有很多问题等待解决。希望在算法、软件开发和人工智能等方向持续探索，并把这个过程记录在这里。' },
]
const awards = [
  'CCPC 中国大学生程序设计竞赛全国邀请赛一等奖',
  '蓝桥杯全国总决赛一等奖',
  '码蹄杯全国职业院校程序设计大赛全国一等奖',
  '团体程序设计天梯赛团体全国一等奖',
  '中国大学生程序设计竞赛高职专场全国二等奖',
  '两次获得国家奖学金，并获得校级一等奖学金、二等奖学金',
]
</script>

<template>
  <Navbar active-page="author" />
  <main class="author-page">
    <section class="author-hero">
      <div class="author-container author-hero-grid">
        <div>
          <span class="author-eyebrow">ABOUT THE AUTHOR</span>
          <h1>Hi，我是田振民 <span aria-hidden="true">👋</span></h1>
          <p class="author-lead">一名软件工程专业学生，也是一名热爱算法、编程与技术探索的开发者。</p>
          <p class="author-story">从职高，到计算机应用技术专科，再通过专升本继续学习软件工程。我把一路上的刷题、竞赛、项目和思考，慢慢整理成这个网站。</p>
          <div class="author-labels"><span>Software Engineering</span><span>Competitive Programming</span><span>Developer</span></div>
          <a class="author-primary-link" href="./code-sharing.html">阅读我的技术文章 <span aria-hidden="true">→</span></a>
        </div>
        <div class="author-portrait" aria-label="田振民的文字头像"><div>田</div><p>写代码，解问题，<br />也记录一路走来的过程。</p></div>
      </div>
    </section>

    <div class="author-container author-content">
      <section class="author-panel" aria-labelledby="about-me-title">
        <span class="author-eyebrow">ABOUT ME</span>
        <h2 id="about-me-title">关于我</h2>
        <p>我目前就读于杭州电子科技大学信息工程学院软件工程专业。本科之前，我曾在浙江机电职业技术大学人工智能学院学习计算机应用技术。我的兴趣并非一开始就明确，而是在一次次学习、训练和实践中逐渐形成。</p>
        <p>算法与程序设计竞赛让我认真理解了用代码解决问题的过程。我曾担任校 ACM 社社长，参加过 CCPC、蓝桥杯、团体程序设计天梯赛、码蹄杯等赛事。除了自己训练，我也参与社团管理、校队训练、竞赛组织和新成员培养。</p>
        <p>现在，我继续学习数据结构、算法、操作系统、计算机组成原理、计算机网络和软件工程，也在尝试开发自己的 ACM / OJ / 技术分享平台。这个网站既是个人主页，也是我的技术成长记录。</p>
      </section>

      <section class="author-panel" aria-labelledby="direction-title">
        <span class="author-eyebrow">WHAT I EXPLORE</span>
        <h2 id="direction-title">关注的技术方向</h2>
        <div class="author-directions"><span v-for="direction in directions" :key="direction">{{ direction }}</span></div>
        <p>我会在这里分享算法题解、竞赛复盘、计算机基础、软件开发实践，以及学习过程中的思考与踩坑记录。</p>
      </section>

      <section class="author-panel" aria-labelledby="journey-title">
        <span class="author-eyebrow">MY JOURNEY</span>
        <h2 id="journey-title">我的经历</h2>
        <div class="author-timeline"><article v-for="stage in journey" :key="stage.number"><span>{{ stage.number }}</span><div><h3>{{ stage.title }}</h3><p>{{ stage.text }}</p></div></article></div>
      </section>

      <section class="author-panel" aria-labelledby="awards-title">
        <span class="author-eyebrow">MILESTONES</span>
        <h2 id="awards-title">竞赛与学习荣誉</h2>
        <ul class="author-awards"><li v-for="award in awards" :key="award">{{ award }}</li></ul>
      </section>

      <section class="author-last-panel">
        <p>保持学习，持续行动，不断成为比昨天更好的自己。</p>
        <strong>Keep Coding. Keep Thinking. Keep Moving.</strong>
        <div><a href="./code-sharing.html">浏览文章 →</a><a href="https://github.com/tzm363636/acm-platform" target="_blank" rel="noopener noreferrer">查看本站 GitHub 仓库 ↗</a></div>
      </section>
    </div>
  </main>
</template>
