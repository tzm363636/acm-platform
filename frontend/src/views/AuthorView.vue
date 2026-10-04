<script setup lang="ts">
import { computed, ref } from 'vue'
import Navbar from '../components/Navbar.vue'

const directions = ['算法与数据结构', 'ACM / ICPC 竞赛', '计算机基础', '软件工程', '后端与 Web 开发', '人工智能探索']
const journey = [
  { number: '01', title: '从职业高中出发', text: '从职高进入计算机应用技术专业，再通过专升本继续学习软件工程。三年里把专业学习放在首位，连续两个学年学测、综测成绩位列班级第一。' },
  { number: '02', title: '遇见算法竞赛', text: '从第一次做算法题，到系统训练搜索、动态规划、图论与数据结构。错误答案、超时和补题，让我学会认真拆解问题。' },
  { number: '03', title: '走向全国赛场', text: '参加 CCPC、蓝桥杯、团体程序设计天梯赛、码蹄杯等赛事，在限时建模、代码实现和团队配合中持续磨练。' },
  { number: '04', title: '从参赛者到 ACM 社社长', text: '统筹策划 20 余场训练、竞赛宣讲和技术交流活动，参与校队训练和新成员培养。' },
  { number: '05', title: '让技术服务更多人', text: '与团队研发并开放校内 OJ 平台，也走进社区开展少儿编程公益教学。技术的价值，在于真正被人使用。' },
  { number: '06', title: '现在，继续向前', text: '继续学习软件工程和计算机基础，探索算法工程、系统研发与人工智能，把过程记录在这个网站。' },
]
const methods = [
  { title: '精准练习', text: '聚焦薄弱知识点，归纳解题模板，复盘易错问题，而不只追求刷题数量。' },
  { title: '拆解难题', text: '把复杂项目或竞赛任务分成可以验证的小步骤，再逐个推进。' },
  { title: '团队协作', text: '主动沟通、合理分工，把个人能力转化为团队的整体效率。' },
  { title: '服务驱动学习', text: '把知识用于平台研发、教学训练和公益实践，让学习有具体的受益者。' },
]
type AwardGroup = '国家级' | '省级 / 赛区' | '国家奖学金'
type Award = { date: string; text: string; group: AwardGroup }
const awards: Award[] = [
  { date: '2024.09', text: '获国家奖学金', group: '国家奖学金' },
  { date: '2025.09', text: '获国家奖学金', group: '国家奖学金' },
  { date: '2024.04', text: '获团体程序设计天梯赛高校排名全国一等奖', group: '国家级' },
  { date: '2025.08', text: '获码蹄杯全国职业院校程序设计大赛国赛一等奖', group: '国家级' },
  { date: '2025.06', text: '获蓝桥杯JAVA/c组全国一等奖', group: '国家级' },
  { date: '2025.06', text: '获CCPC中国大学生程序设计竞赛（郑州）全国邀请赛一等奖', group: '国家级' },
  { date: '2025.04', text: '获团体程序设计天梯赛团体排名全国一等奖', group: '国家级' },
  { date: '2024.04', text: '获团体程序设计天梯赛团体全国二等奖', group: '国家级' },
  { date: '2024.07', text: '获码蹄杯全国职业院校程序设计大赛国赛二等奖', group: '国家级' },
  { date: '2024.04', text: '获团体程序设计天梯赛高校排名全国二等奖', group: '国家级' },
  { date: '2024.11', text: '获CCPC中国大学生程序设计竞赛（高职专场）二等奖', group: '国家级' },
  { date: '2025.04', text: '获团体程序设计天梯赛个人排名全国三等奖', group: '国家级' },
  { date: '2025.07', text: '获码蹄杯全国职业院校程序设计大赛华东赛区一等奖', group: '省级 / 赛区' },
  { date: '2024.04', text: '获团体程序设计天梯赛高校排名浙江省一等奖', group: '省级 / 赛区' },
  { date: '2024.06', text: '获码蹄杯全国职业院校程序设计大赛省赛一等奖', group: '省级 / 赛区' },
  { date: '2025.04', text: '获蓝桥杯JAVA/c组浙江省一等奖', group: '省级 / 赛区' },
  { date: '2025.04', text: '获浙江省大学生程序设计竞赛一等奖', group: '省级 / 赛区' },
  { date: '2024.11', text: '获第七届传智杯全国IT技能大赛一等奖', group: '国家级' },
  { date: '2024.04', text: '获团体程序设计天梯赛团体浙江省二等奖', group: '省级 / 赛区' },
  { date: '2024.04', text: '获浙江省大学生程序设计竞赛省二等奖', group: '省级 / 赛区' },
  { date: '2025.04', text: '获团体程序设计天梯赛团体浙江省二等奖', group: '省级 / 赛区' },
  { date: '2025.04', text: '获团体程序设计天梯赛高校浙江省二等奖', group: '省级 / 赛区' },
  { date: '2024.04', text: '获蓝桥杯JAVA/c组浙江省三等奖', group: '省级 / 赛区' },
]
const awardFilters = ['全部', '国家级', '省级 / 赛区', '国家奖学金'] as const
const activeAwardFilter = ref<(typeof awardFilters)[number]>('全部')
const visibleAwards = computed(() => activeAwardFilter.value === '全部' ? awards : awards.filter((award) => award.group === activeAwardFilter.value))
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
          <p class="author-story">从职高，到计算机应用技术专科，再通过专升本继续学习软件工程。我把一路上的学习、算法竞赛、项目实践和思考，慢慢整理成这个网站。</p>
          <div class="author-labels"><span>Software Engineering</span><span>Competitive Programming</span><span>Developer</span></div>
          <a class="author-primary-link" href="./code-sharing.html">阅读我的技术文章 <span aria-hidden="true">→</span></a>
        </div>
        <div class="author-portrait" aria-label="田振民的文字头像"><div>田</div><p>写代码，解问题，<br />也记录一路走来的过程。</p></div>
      </div>
    </section>

    <div class="author-container author-content">
      <div class="author-highlights" aria-label="学习与实践数据">
        <div><strong>2 次</strong><span>国家奖学金</span></div>
        <div><strong>2500+</strong><span>算法题目</span></div>
        <div><strong>1 万+</strong><span>个人代码提交</span></div>
        <div><strong>1600+</strong><span>校内 OJ 服务学生</span></div>
      </div>

      <section class="author-panel" aria-labelledby="about-me-title">
        <span class="author-eyebrow">ABOUT ME</span>
        <h2 id="about-me-title">以学为本，夯实根基</h2>
        <p>我目前就读于杭州电子科技大学信息工程学院软件工程专业。本科之前，我曾在浙江机电职业技术大学人工智能学院学习计算机应用技术。从职高到专科，再通过专升本继续学习，我始终把专业基础放在重要的位置。</p>
        <p>连续两个学年，我的学测和综测成绩均位列班级第一，并在 2024 年 9 月、2025 年 9 月两次获得国家奖学金。对我来说，学习不仅是追求分数，更是在积累分析问题、解决问题的能力，为竞赛和工程实践打好基础。</p>
      </section>

      <section class="author-panel" aria-labelledby="competition-title">
        <span class="author-eyebrow">COMPETITIVE PROGRAMMING</span>
        <h2 id="competition-title">以赛砺技，精进笃行</h2>
        <p>大学期间，我坚持系统化训练，累计完成 2500 道算法题，个人代码提交量突破万次，个人 rating 排名跻身全国前 6%。从一次次 Wrong Answer、超时和赛后补题，到限时建模、选择算法并完成实现，竞赛让我学会不断复盘和改进。</p>
        <p>我参加过蓝桥杯、CCPC、码蹄杯、团体程序设计天梯赛等赛事。团队比赛也让我懂得，沟通、分工与配合和个人解题能力一样重要。具体荣誉按时间列在下方。</p>
      </section>

      <section class="author-panel author-project-panel" aria-labelledby="project-title">
        <div>
          <span class="author-eyebrow">PROJECT IN PRACTICE</span>
          <h2 id="project-title">深耕实践，让技术落地</h2>
          <p>我与团队自主研发并对校内开放 OJ 平台，将练习、教学和竞赛串联起来。项目中，我们实现了代码查重、封榜回溯等功能。我参与了产品规划、前后端开发、运维优化和基于用户反馈的迭代，也由此开始从竞赛训练走向完整的软件工程实践。</p>
        </div>
        <div class="author-project-stats" aria-label="校内 OJ 项目数据">
          <div><strong>1600+</strong><span>服务学生</span></div>
          <div><strong>15 万+</strong><span>平台代码提交</span></div>
          <div><strong>160 场</strong><span>公开训练赛</span></div>
        </div>
      </section>

      <section class="author-panel" aria-labelledby="community-title">
        <span class="author-eyebrow">COMMUNITY & SERVICE</span>
        <h2 id="community-title">躬身服务，聚力成长</h2>
        <p>担任校 ACM 社社长期间，我统筹策划 20 余场专业训练、竞赛宣讲和技术交流活动，参与校队训练与新成员培养。除了社团工作，我还走进社区开展少儿编程公益教学，希望把所学用于帮助更多人接触编程。</p>
        <p>这些经历让我更加重视组织协调、沟通与共情，也让我相信，一个好的学习社区需要清晰的资料、持续的训练和愿意互相帮助的人。</p>
      </section>

      <section class="author-panel" aria-labelledby="journey-title">
        <span class="author-eyebrow">MY JOURNEY</span>
        <h2 id="journey-title">我的经历</h2>
        <div class="author-timeline"><article v-for="stage in journey" :key="stage.number"><span>{{ stage.number }}</span><div><h3>{{ stage.title }}</h3><p>{{ stage.text }}</p></div></article></div>
      </section>

      <section class="author-panel" aria-labelledby="awards-title">
        <span class="author-eyebrow">MILESTONES</span>
        <h2 id="awards-title">竞赛与学习荣誉</h2>
        <div class="author-award-filters" aria-label="筛选荣誉">
          <button v-for="filter in awardFilters" :key="filter" type="button" :class="{ active: activeAwardFilter === filter }" :aria-pressed="activeAwardFilter === filter" @click="activeAwardFilter = filter">{{ filter }}</button>
        </div>
        <ul class="author-awards"><li v-for="award in visibleAwards" :key="`${award.date}-${award.text}`"><time>{{ award.date }}</time><span>{{ award.group }}</span><strong>{{ award.text }}</strong></li></ul>
      </section>

      <section class="author-panel" aria-labelledby="methods-title">
        <span class="author-eyebrow">HOW I KEEP LEARNING</span>
        <h2 id="methods-title">沉淀方法，持续深耕</h2>
        <div class="author-methods"><article v-for="method in methods" :key="method.title"><h3>{{ method.title }}</h3><p>{{ method.text }}</p></article></div>
      </section>

      <section class="author-panel" aria-labelledby="future-title">
        <span class="author-eyebrow">WHAT COMES NEXT</span>
        <h2 id="future-title">逐光致远，续写新章</h2>
        <p>我是一名中共党员，也是一名仍在学习中的软件工程学生。我希望坚持“学以致用、回馈社会”，继续提升技术能力，在算法工程和系统化工程实践中深入探索，参与更多真实项目，也继续推广编程教育。</p>
        <div class="author-directions"><span v-for="direction in directions" :key="direction">{{ direction }}</span></div>
        <p>这个网站会继续记录我的算法题解、竞赛复盘、计算机基础、项目实践和成长思考。</p>
      </section>

      <section class="author-last-panel">
        <p>星光不问赶路人，时间不负有心人。</p>
        <strong>Keep Coding. Keep Thinking. Keep Moving.</strong>
        <div><a href="./code-sharing.html">浏览文章 →</a><a href="https://github.com/tzm363636/acm-platform" target="_blank" rel="noopener noreferrer">查看本站 GitHub 仓库 ↗</a></div>
      </section>
    </div>
  </main>
</template>
