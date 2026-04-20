<script setup lang="ts">
import { computed } from 'vue'
import { Document, Food, Link, Lightning, PieChart, Reading, Tickets, VideoPlay } from '@element-plus/icons-vue'

const nutrientCards = [
  {
    title: '优质蛋白',
    subtitle: '帮助维持肌肉、修复组织、增强饱腹感',
    points: ['鸡胸肉、鱼虾、鸡蛋、牛奶、豆腐', '每餐尽量有一类蛋白来源', '运动后优先补蛋白更稳妥'],
    accent: '#f97316'
  },
  {
    title: '复合碳水',
    subtitle: '提供稳定能量，减少血糖波动太快',
    points: ['燕麦、玉米、红薯、糙米、全麦面包', '主食不建议完全不吃', '粗细搭配比单吃精米面更均衡'],
    accent: '#f59e0b'
  },
  {
    title: '健康脂肪',
    subtitle: '帮助吸收脂溶性维生素，也影响激素与饱腹感',
    points: ['牛油果、坚果、深海鱼、橄榄油', '控制总量比完全拒绝更重要', '避免长期高频油炸食品'],
    accent: '#fb7185'
  },
  {
    title: '膳食纤维',
    subtitle: '帮助肠道健康，也能提升餐后满足感',
    points: ['绿叶菜、菌菇、豆类、水果、燕麦', '一餐至少有半盘蔬菜更容易达标', '果汁不等于水果本身'],
    accent: '#34d399'
  }
] as const

const foodTopics = [
  {
    group: '日常主食',
    title: '会影响饱腹感和热量节奏',
    summary: '主食不是越少越好，关键在于份量和选择。精米面吃得快、饿得也快，粗粮和薯类更适合做搭配。',
    foods: [
      { name: '米饭', feature: '以碳水为主', detail: '适合搭配蛋白质和蔬菜一起吃，避免单吃一大碗。' },
      { name: '红薯', feature: '碳水 + 一定膳食纤维', detail: '饱腹感更强，但同样属于主食，不能无限量吃。' },
      { name: '燕麦', feature: '复合碳水 + 纤维', detail: '适合作为早餐底盘，搭牛奶和鸡蛋更均衡。' }
    ]
  },
  {
    group: '高蛋白食物',
    title: '适合稳定三餐结构',
    summary: '如果你经常饿得快、恢复慢、总想吃零食，往往和蛋白质安排不够有关。',
    foods: [
      { name: '鸡蛋', feature: '蛋白质 + 脂肪', detail: '早餐很好用，但别只吃蛋不吃其他食物。' },
      { name: '鸡胸肉', feature: '高蛋白、低脂', detail: '适合减脂期做主蛋白来源，但烹饪尽量少油。' },
      { name: '豆腐', feature: '植物蛋白', detail: '适合和蔬菜搭配，也适合晚餐做清淡菜。' }
    ]
  },
  {
    group: '零食与饮品',
    title: '最容易被低估的隐形热量',
    summary: '很多人主餐并不夸张，但奶茶、甜饮、饼干和坚果把总热量悄悄拉高了。',
    foods: [
      { name: '奶茶', feature: '糖 + 脂肪 + 热量密度高', detail: '一杯就可能顶半餐，建议降糖、减配料、少频率。' },
      { name: '坚果', feature: '健康脂肪丰富', detail: '少量很有价值，但一把接一把很容易超量。' },
      { name: '酸奶', feature: '蛋白质或糖分差异很大', detail: '优先看配料表，低糖高蛋白版本更适合日常。' }
    ]
  },
  {
    group: '蔬菜与水果',
    title: '帮助补足纤维、维生素和餐盘体积',
    summary: '这一类食物最适合用来优化整餐结构。吃得不够时，常见问题不是热量超标，而是饱腹感不稳、微量营养素不足。',
    foods: [
      { name: '西兰花', feature: '纤维 + 维生素 C', detail: '适合和鸡胸肉、牛肉这类蛋白搭配，能把一餐的体积和满足感一起拉起来。' },
      { name: '番茄', feature: '水分高、热量低', detail: '适合做配菜或加进汤里，帮助减少整餐过油过腻的感觉。' },
      { name: '苹果', feature: '整果更有饱腹感', detail: '适合作为加餐或早餐补充，优先整颗吃，比榨成果汁更稳妥。' }
    ]
  }
] as const

const sceneAdvice = [
  {
    scene: '早餐',
    title: '先补能量，再稳住上午饥饿感',
    advice: '推荐用“主食 + 蛋白质 + 水果/蔬菜”的组合，例如燕麦牛奶加鸡蛋，再配一个苹果。'
  },
  {
    scene: '午餐',
    title: '最适合把结构吃完整的一餐',
    advice: '盘子可以按“半盘蔬菜、四分之一主食、四分之一蛋白质”去搭，外卖也尽量往这个方向靠。'
  },
  {
    scene: '晚餐',
    title: '别一味不吃，重在减少油重和过量主食',
    advice: '如果晚上活动少，可以比午餐更清淡，但仍然建议保留少量主食和足够蛋白。'
  },
  {
    scene: '加餐',
    title: '用于补缺口，不是额外奖励',
    advice: '下午很饿时优先选酸奶、鸡蛋、水果或少量坚果，而不是高糖饮料和高油点心。'
  }
] as const

const myths = [
  {
    wrong: '减脂就完全不能吃碳水',
    right: '真正更重要的是总量、时机和主食质量。完全断碳更容易反扑和暴食。'
  },
  {
    wrong: '只要是“粗粮”就能放心吃',
    right: '粗粮也有热量，粗细搭配更合理，关键还是控制一餐总量。'
  },
  {
    wrong: '水果随便吃，不会胖',
    right: '水果有维生素，但也含糖。整果优于果汁，晚上大量吃同样会抬高摄入。'
  },
  {
    wrong: '不吃晚饭就一定减重更快',
    right: '长期跳过晚餐可能导致第二天更饿，反而更难稳定控制饮食。'
  }
] as const

const practicalRules = computed(() => [
  '一餐至少保留一种蛋白质来源，让饱腹感更稳。',
  '主食优先考虑粗细搭配，不用极端地完全戒掉。',
  '蔬菜尽量覆盖两种颜色，越容易把纤维和微量营养素补齐。',
  '零食和饮料先看频率，再看份量，隐形热量通常出现在这里。'
])

const officialResources = [
  {
    tag: '官方发布',
    title: '《中国居民膳食指南（2022）》发布页',
    summary: '适合先建立总览，了解膳食指南为什么重要，以及它的整体框架。',
    source: '中国营养学会',
    href: 'https://dg.cnsoc.org/article/04/glVJd6DRRCqm-hYzWlEVNQ.html'
  },
  {
    tag: '准则解读',
    title: '多吃蔬果、奶类、全谷、大豆',
    summary: '如果你最常纠结“水果能不能多吃”“奶制品怎么选”，这一篇最值得先看。',
    source: '中国营养学会',
    href: 'https://dg.cnsoc.org/article/04/70JvPbFmTlyZbjoO67LeRg.html'
  },
  {
    tag: '清淡饮食',
    title: '少盐少油，控糖限酒',
    summary: '适合配合减脂、控糖和日常做饭一起看，里面有更明确的盐、油、糖建议。',
    source: '中国营养学会',
    href: 'https://dg.cnsoc.org/article/04/ApX3_ozGTmSoqQaFFh5z_Q.html'
  },
  {
    tag: '进餐规律',
    title: '规律进餐，足量饮水',
    summary: '如果你经常漏早餐、晚饭过晚或者喝饮料代替喝水，这篇更贴近日常习惯调整。',
    source: '中国营养学会',
    href: 'https://dg.cnsoc.org/article/04/wDCyy7cWSJCN6pwKHOo5Dw.html'
  }
] as const

const videoResources = [
  {
    tag: '精选视频',
    title: '最新「吃饭指南」来了！我帮你总结了5大关键点！',
    summary: '偏入门向，适合先建立整体框架，再回到页面里的食物卡片对照理解。',
    creator: '营养师顾中一 / 万物研究所',
    href: 'https://www.bilibili.com/video/BV1qA4y1X71k/'
  },
  {
    tag: '营养课程',
    title: '李熙博士解读《中国居民膳食指南2022》（第一部分）',
    summary: '系列化讲解更系统，适合你想从“知道建议”进阶到“理解为什么这样吃”。',
    creator: '阿波的小破站',
    href: 'https://www.bilibili.com/video/BV1MTwHeiEnc/'
  },
  {
    tag: '三餐实践',
    title: '《中国居民膳食指南》一日三餐',
    summary: '更偏实际搭配，适合看完理论后，找找三餐落地时的思路和灵感。',
    creator: 'ssteff',
    href: 'https://www.bilibili.com/video/BV1n84y1q7Ko/'
  },
  {
    tag: '低卡实操',
    title: '低卡低脂食物大全，减脂期可以吃哪些食物？',
    summary: '更适合做日常挑食材时的速查，能和页面里的食物分类形成互补。',
    creator: '养生带饭',
    href: 'https://www.bilibili.com/video/BV1AXQdYiE1F/'
  }
] as const
</script>

<template>
  <div class="science-page">
    <section class="hero-card glass-effect">
      <div class="hero-copy">
        <span class="eyebrow">饮食科普</span>
        <h2>从看懂营养素，到学会挑主食、蛋白和零食，把常见饮食知识和实用资源整理成一页。</h2>
        <p>你可以先用这页快速扫一遍重点，再跳到官方解读或精选视频继续看，适合做日常饮食记录前后的对照参考。</p>
      </div>

      <div class="hero-aside">
        <div class="hero-chip">
          <el-icon><Reading /></el-icon>
          饮食学习入口
        </div>
        <div class="hero-grid">
          <div class="hero-box">
            <span>覆盖主题</span>
            <strong>成分与搭配</strong>
          </div>
          <div class="hero-box">
            <span>延伸阅读</span>
            <strong>{{ officialResources.length }} 条官方资料</strong>
          </div>
          <div class="hero-box">
            <span>视频入口</span>
            <strong>{{ videoResources.length }} 个精选视频</strong>
          </div>
          <div class="hero-box">
            <span>适合场景</span>
            <strong>随看随用</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="resource-grid">
      <el-card class="resource-card glass-effect">
        <template #header>
          <div class="card-title">
            <el-icon><Link /></el-icon>
            <span>官方延伸阅读</span>
          </div>
        </template>

        <div class="resource-list">
          <a
            v-for="item in officialResources"
            :key="item.href"
            class="resource-item"
            :href="item.href"
            target="_blank"
            rel="noreferrer"
          >
            <span class="resource-tag">{{ item.tag }}</span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.summary }}</p>
            <small>{{ item.source }}</small>
          </a>
        </div>
      </el-card>

      <el-card class="resource-card glass-effect">
        <template #header>
          <div class="card-title">
            <el-icon><VideoPlay /></el-icon>
            <span>视频推荐</span>
          </div>
        </template>

        <div class="resource-list">
          <a
            v-for="item in videoResources"
            :key="item.href"
            class="resource-item"
            :href="item.href"
            target="_blank"
            rel="noreferrer"
          >
            <span class="resource-tag video">{{ item.tag }}</span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.summary }}</p>
            <small>{{ item.creator }}</small>
          </a>
        </div>
      </el-card>
    </section>

    <section class="nutrient-grid">
      <article v-for="card in nutrientCards" :key="card.title" class="nutrient-card glass-effect">
        <div class="accent-bar" :style="{ background: card.accent }"></div>
        <div class="nutrient-copy">
          <span>{{ card.title }}</span>
          <strong>{{ card.subtitle }}</strong>
          <ul>
            <li v-for="point in card.points" :key="point">{{ point }}</li>
          </ul>
        </div>
      </article>
    </section>

    <section class="content-grid">
      <el-card class="topic-card glass-effect">
        <template #header>
          <div class="card-title">
            <el-icon><Food /></el-icon>
            <span>常见食物成分</span>
          </div>
        </template>

        <div class="topic-list">
          <article v-for="topic in foodTopics" :key="topic.group" class="topic-item">
            <div class="topic-head">
              <span class="topic-kicker">{{ topic.group }}</span>
              <h3>{{ topic.title }}</h3>
              <p>{{ topic.summary }}</p>
            </div>

            <div class="food-list">
              <div v-for="food in topic.foods" :key="food.name" class="food-card">
                <div class="food-meta">
                  <strong>{{ food.name }}</strong>
                  <span>{{ food.feature }}</span>
                </div>
                <p>{{ food.detail }}</p>
              </div>
            </div>
          </article>
        </div>
      </el-card>

      <div class="side-column">
        <el-card class="rule-card glass-effect">
          <template #header>
            <div class="card-title">
              <el-icon><Document /></el-icon>
              <span>实用搭配原则</span>
            </div>
          </template>

          <div class="rule-list">
            <div v-for="rule in practicalRules" :key="rule" class="rule-item">
              <span class="bullet"></span>
              <p>{{ rule }}</p>
            </div>
          </div>
        </el-card>

        <el-card class="rule-card glass-effect">
          <template #header>
            <div class="card-title">
              <el-icon><Lightning /></el-icon>
              <span>常见误区</span>
            </div>
          </template>

          <div class="myth-list">
            <article v-for="myth in myths" :key="myth.wrong" class="myth-item">
              <span class="wrong">误区</span>
              <p>{{ myth.wrong }}</p>
              <span class="right">建议</span>
              <p>{{ myth.right }}</p>
            </article>
          </div>
        </el-card>
      </div>
    </section>

    <section class="scene-grid">
      <el-card class="scene-card glass-effect">
        <template #header>
          <div class="card-title">
            <el-icon><Tickets /></el-icon>
            <span>按场景看建议</span>
          </div>
        </template>

        <div class="scene-list">
          <article v-for="scene in sceneAdvice" :key="scene.scene" class="scene-item">
            <span class="scene-tag">{{ scene.scene }}</span>
            <strong>{{ scene.title }}</strong>
            <p>{{ scene.advice }}</p>
          </article>
        </div>
      </el-card>

      <el-card class="scene-card glass-effect">
        <template #header>
          <div class="card-title">
            <el-icon><PieChart /></el-icon>
            <span>怎么把科普用在记录里</span>
          </div>
        </template>

        <div class="steps-list">
          <div class="step-item">
            <span>1</span>
            <p>先在“识别记录”里上传一餐，看看系统估算出的热量和三大营养素。</p>
          </div>
          <div class="step-item">
            <span>2</span>
            <p>再回到这里，对照这餐主要食材属于哪一类，看看是不是蛋白不足、主食偏多或者油脂偏高。</p>
          </div>
          <div class="step-item">
            <span>3</span>
            <p>最后结合“饮食报告”里的近 7 天趋势，判断自己是偶尔一餐失衡，还是整体习惯需要调整。</p>
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>

<style scoped lang="scss">
.science-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 16px;
}

.hero-card {
  padding: 24px 26px;
  border-radius: 30px;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 18px;
  background:
    radial-gradient(circle at top left, rgba(255, 232, 211, 0.96), transparent 42%),
    linear-gradient(135deg, rgba(255, 250, 244, 0.98), rgba(255, 239, 223, 0.88));
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: #fff0e1;
  color: #9a3412;
  font-size: 12px;
  font-weight: 800;
}

.hero-copy h2 {
  margin: 12px 0 10px;
  font-size: clamp(28px, 2.6vw, 38px);
  line-height: 1.15;
  color: #7c2d12;
}

.hero-copy p {
  margin: 0;
  color: #7c2d12;
  line-height: 1.8;
}

.hero-aside {
  display: grid;
  gap: 14px;
}

.hero-chip {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  color: #9a3412;
  font-size: 13px;
  font-weight: 800;
}

.hero-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-box,
.food-card,
.scene-item,
.step-item {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.8);
}

.hero-box {
  padding: 16px 18px;
  display: grid;
  gap: 8px;
}

.hero-box span,
.food-meta span,
.topic-kicker,
.scene-tag {
  color: #6b7280;
  font-size: 12px;
}

.hero-box strong,
.food-meta strong,
.scene-item strong {
  color: #7c2d12;
}

.hero-box strong {
  font-size: 22px;
}

.nutrient-grid,
.resource-grid,
.content-grid,
.scene-grid {
  display: grid;
  gap: 16px;
}

.resource-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.resource-list {
  display: grid;
  gap: 12px;
}

.resource-item {
  text-decoration: none;
  color: inherit;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 250, 244, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.9);
  display: grid;
  gap: 8px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.resource-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 30px rgba(249, 115, 22, 0.12);
  border-color: #ffd7b1;
}

.resource-tag {
  display: inline-flex;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fff0e1;
  color: #b45309;
  font-size: 12px;
  font-weight: 800;
}

.resource-tag.video {
  background: #ffe4e6;
  color: #be185d;
}

.resource-item strong {
  color: #7c2d12;
  font-size: 18px;
  line-height: 1.5;
}

.resource-item p {
  margin: 0;
  color: #6b7280;
  line-height: 1.8;
}

.resource-item small {
  color: #9ca3af;
}

.nutrient-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.nutrient-card {
  padding: 20px 22px;
  display: grid;
  grid-template-columns: 12px 1fr;
  gap: 16px;
}

.accent-bar {
  width: 12px;
  border-radius: 999px;
}

.nutrient-copy {
  display: grid;
  gap: 8px;
}

.nutrient-copy span {
  color: #9a3412;
  font-size: 13px;
  font-weight: 800;
}

.nutrient-copy strong {
  color: #7c2d12;
  font-size: 22px;
  line-height: 1.4;
}

.nutrient-copy ul {
  margin: 0;
  padding-left: 18px;
  color: #6b7280;
  line-height: 1.8;
}

.content-grid {
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.75fr);
}

.side-column {
  display: grid;
  gap: 16px;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #7c2d12;
}

.topic-list,
.scene-list,
.rule-list,
.myth-list {
  display: grid;
  gap: 14px;
}

.topic-item {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 250, 244, 0.86);
  display: grid;
  gap: 14px;
}

.topic-head h3 {
  margin: 8px 0 6px;
  color: #7c2d12;
  font-size: 22px;
}

.topic-head p,
.food-card p,
.rule-item p,
.myth-item p,
.scene-item p,
.step-item p {
  margin: 0;
  color: #6b7280;
  line-height: 1.8;
}

.food-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.food-card {
  padding: 14px 16px;
  display: grid;
  gap: 10px;
}

.food-meta {
  display: grid;
  gap: 4px;
}

.rule-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 240, 225, 0.76);
}

.bullet {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #ff7a18;
  margin-top: 8px;
}

.myth-item {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 250, 244, 0.86);
  display: grid;
  gap: 6px;
}

.wrong,
.right,
.scene-tag {
  display: inline-flex;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  font-weight: 800;
}

.wrong {
  background: #fff1f2;
  color: #e11d48;
}

.right {
  background: #ecfdf5;
  color: #059669;
}

.scene-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.scene-item {
  padding: 16px 18px;
  display: grid;
  gap: 10px;
}

.scene-tag {
  background: #fff0e1;
  color: #9a3412;
}

.steps-list {
  display: grid;
  gap: 12px;
}

.step-item {
  padding: 16px 18px;
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 12px;
  align-items: start;
}

.step-item span {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(120deg, #ff9838, #ff7a18);
  color: #fff;
  font-weight: 800;
}

@media (max-width: 1180px) {
  .hero-card,
  .resource-grid,
  .content-grid,
  .scene-grid {
    grid-template-columns: 1fr;
  }

  .food-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .nutrient-grid,
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    padding: 20px;
  }
}
</style>
