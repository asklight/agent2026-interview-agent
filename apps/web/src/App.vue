<template>
  <main class="app-shell">
    <section class="topbar">
      <div>
        <h1>北洋 Java 面试官</h1>
        <p>Java 后端技术面试训练智能体</p>
      </div>
      <el-button :loading="loading" type="primary" @click="checkHealth">
        检查后端
      </el-button>
    </section>

    <section class="status-band">
      <el-alert
        :closable="false"
        :title="statusTitle"
        :type="healthy ? 'success' : 'warning'"
        show-icon
      />
    </section>

    <section class="mode-grid">
      <article v-for="mode in modes" :key="mode.title" class="mode-card">
        <div class="mode-card__tag">{{ mode.tag }}</div>
        <h2>{{ mode.title }}</h2>
        <p>{{ mode.description }}</p>
        <el-button disabled>{{ mode.action }}</el-button>
      </article>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getHealth } from '@/api/modules/health'

const loading = ref(false)
const healthMessage = ref('')
const healthy = ref(false)

const modes = [
  {
    tag: 'P0',
    title: 'Java 核心知识面试',
    description: '围绕 Java、MySQL、Redis、Spring 等核心知识进行提问、追问和复盘。',
    action: '下一步实现',
  },
  {
    tag: 'P1',
    title: '项目经历深挖',
    description: '根据项目经历识别技术栈，模拟真实面试官的项目追问。',
    action: '规划中',
  },
  {
    tag: 'P1',
    title: 'Milvus 面经增强',
    description: '从八股总结和面经知识库中召回相关资料，增强追问和复盘。',
    action: '规划中',
  },
]

const statusTitle = computed(() => {
  if (healthy.value) return `后端已连接：${healthMessage.value}`
  if (healthMessage.value) return healthMessage.value
  return '等待后端健康检查'
})

async function checkHealth() {
  loading.value = true
  try {
    const response = await getHealth()
    healthMessage.value = response.data.data
    healthy.value = true
  } catch {
    healthMessage.value = '后端暂不可用，请确认 Spring Boot 已启动'
    healthy.value = false
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  checkHealth()
})
</script>
