<template>
  <main class="page-frame simulation-setup">
    <RouterLink class="back-link" to="/">← 返回训练首页</RouterLink>
    <header class="workspace-heading simulation-setup__heading">
      <div><p class="page-kicker">FULL INTERVIEW</p><h1>综合模拟面试</h1></div>
      <p>一场连续的技术面试，依次讨论项目、Java 基础与算法思路。过程中不展示评分，结束后统一复盘。</p>
    </header>

    <section v-if="loading" class="simulation-options-state">正在准备面试选项...</section>
    <section v-else-if="error" class="simulation-options-state"><p>{{ error }}</p><el-button @click="load">重新加载</el-button></section>
    <template v-else-if="options">
      <section v-if="!options.projects.length" class="simulation-empty-project">
        <Briefcase /><div><h2>先准备一份项目档案</h2><p>综合面试需要从你确认过的真实项目出发，避免生成泛泛的项目问题。</p></div>
        <RouterLink class="primary-link" to="/project-deep-dive/new">创建项目档案</RouterLink>
      </section>
      <form v-else class="simulation-config" @submit.prevent="start">
        <section class="simulation-config__main">
          <div class="simulation-field">
            <label>用于深挖的项目</label>
            <div class="simulation-project-list">
              <button v-for="project in options.projects" :key="project.id" type="button"
                :class="{ selected: form.projectProfileId === project.id }" @click="form.projectProfileId = project.id">
                <span><strong>{{ project.name || '未命名项目' }}</strong><small>{{ project.summary }}</small></span>
                <i>{{ project.techStack.slice(0, 3).join(' · ') }}</i>
              </button>
            </div>
          </div>
          <div class="simulation-field-grid">
            <label>基础问答范围<el-select v-model="form.knowledgeModule"><el-option v-for="item in options.knowledgeModules" :key="item" :label="moduleName(item)" :value="item" /></el-select></label>
            <label>整体难度<el-select v-model="form.difficulty"><el-option v-for="item in options.difficulties" :key="item" :label="difficultyName(item)" :value="item" /></el-select></label>
          </div>
          <div class="simulation-field">
            <label>算法讨论题</label>
            <el-select v-model="form.algorithmProblemId" filterable class="full-width">
              <el-option v-for="problem in options.algorithmProblems" :key="problem.id" :label="`${problem.title} · ${difficultyName(problem.difficulty)}`" :value="problem.id" />
            </el-select>
          </div>
        </section>
        <aside class="simulation-brief">
          <p class="page-kicker">INTERVIEW PLAN</p><h2>约 20 分钟</h2>
          <ol><li><span>01</span><div><strong>项目交流</strong><small>围绕真实经历连续追问</small></div></li><li><span>02</span><div><strong>基础问答</strong><small>两道短问题校准基本功</small></div></li><li><span>03</span><div><strong>算法讨论</strong><small>口述方案、复杂度与边界</small></div></li></ol>
          <p>现在使用文字作答；输入协议已保留语音转写类型，后续可直接接入沉浸式语音。</p>
          <el-button native-type="submit" type="primary" :loading="creating" :disabled="!ready"><VideoPlay />进入面试</el-button>
        </aside>
      </form>
    </template>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { Briefcase, VideoPlay } from '@element-plus/icons-vue'
import { createSimulation, getSimulationOptions } from '../api/simulationApi'
import type { SimulationOptions } from '../model/types'

const router = useRouter()
const options = ref<SimulationOptions | null>(null)
const loading = ref(false)
const creating = ref(false)
const error = ref('')
const form = reactive({ projectProfileId: 0, algorithmProblemId: 0, knowledgeModule: 'JAVA', difficulty: 'MEDIUM' })
const clientRequestId = crypto.randomUUID()
const ready = computed(() => form.projectProfileId > 0 && form.algorithmProblemId > 0 && Boolean(form.knowledgeModule && form.difficulty))

onMounted(load)
async function load() {
  loading.value = true; error.value = ''
  try {
    options.value = (await getSimulationOptions()).data.data
    form.projectProfileId ||= options.value.projects[0]?.id ?? 0
    form.algorithmProblemId ||= options.value.algorithmProblems.find(item => item.difficulty === 'medium')?.id ?? options.value.algorithmProblems[0]?.id ?? 0
    form.knowledgeModule = options.value.knowledgeModules[0] ?? 'JAVA'
    form.difficulty = options.value.difficulties.includes('MEDIUM') ? 'MEDIUM' : options.value.difficulties[0] ?? 'MEDIUM'
  } catch { error.value = '面试配置暂时无法加载，请稍后重试。' }
  finally { loading.value = false }
}
async function start() {
  if (!ready.value) return
  creating.value = true
  try { const result = (await createSimulation({ clientRequestId, ...form })).data.data; await router.push(`/simulation/${result.simulationId}`) }
  catch { error.value = '面试暂时无法创建，请检查项目档案后重试。' }
  finally { creating.value = false }
}
function moduleName(value: string) { return ({ JAVA: 'Java 核心', MYSQL: 'MySQL', REDIS: 'Redis', SPRING: 'Spring' } as Record<string, string>)[value] ?? value }
function difficultyName(value: string) { return ({ EASY: '基础', MEDIUM: '进阶', HARD: '高阶', easy: '简单', medium: '中等', hard: '困难' } as Record<string, string>)[value] ?? value }
</script>
