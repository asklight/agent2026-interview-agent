<template>
  <main class="page-frame algorithm-library">
    <header class="workspace-heading">
      <div><p class="page-kicker">ALGORITHM ORAL PRACTICE</p><h1>算法口述训练</h1></div>
      <p>像面试一样把思路讲清楚。面试官会按澄清、方案、优化、复杂度和边界逐步追问，结束后再统一复盘。</p>
    </header>

    <div class="algorithm-filters" role="group" aria-label="难度筛选">
      <button v-for="item in difficulties" :key="item.value" type="button"
        :class="{ active: difficulty === item.value }" @click="difficulty = item.value">
        {{ item.label }}
      </button>
    </div>

    <section v-if="loading" class="algorithm-library__state">正在加载题目...</section>
    <section v-else-if="error" class="algorithm-library__state">
      <p>{{ error }}</p><el-button @click="load">重新加载</el-button>
    </section>
    <section v-else class="algorithm-problem-list" aria-label="算法题目">
      <article v-for="problem in visibleProblems" :key="problem.id" class="algorithm-problem-item">
        <div class="algorithm-problem-item__main">
          <span :class="`difficulty difficulty--${problem.difficulty}`">{{ difficultyLabel(problem.difficulty) }}</span>
          <h2>{{ problem.title }}</h2>
          <p>{{ problem.statement }}</p>
          <div><span v-for="tag in problem.tags" :key="tag">{{ tag }}</span></div>
        </div>
        <el-button type="primary" :loading="creatingId === problem.id" :disabled="creatingId !== null"
          @click="start(problem.id)">开始口述 <Right /></el-button>
      </article>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Right } from '@element-plus/icons-vue'
import { createAlgorithmSession, getAlgorithmProblems } from '../api/algorithmApi'
import type { AlgorithmDifficulty, AlgorithmProblem } from '../model/types'

const router = useRouter()
const problems = ref<AlgorithmProblem[]>([])
const difficulty = ref('all')
const loading = ref(false)
const creatingId = ref<number | null>(null)
const error = ref('')
const difficulties = [
  { value: 'all', label: '全部' }, { value: 'easy', label: '简单' },
  { value: 'medium', label: '中等' }, { value: 'hard', label: '困难' },
]
const visibleProblems = computed(() => difficulty.value === 'all'
  ? problems.value : problems.value.filter(problem => problem.difficulty === difficulty.value))

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try { problems.value = (await getAlgorithmProblems()).data.data }
  catch { error.value = '暂时无法加载题目，请稍后重试。' }
  finally { loading.value = false }
}

async function start(problemId: number) {
  creatingId.value = problemId
  try {
    const session = (await createAlgorithmSession(problemId)).data.data
    await router.push({ name: 'algorithm-room', params: { sessionId: session.sessionId } })
  } catch { error.value = '创建训练失败，请稍后重试。' }
  finally { creatingId.value = null }
}

function difficultyLabel(value: AlgorithmDifficulty) {
  return { easy: '简单', medium: '中等', hard: '困难' }[value]
}
</script>
