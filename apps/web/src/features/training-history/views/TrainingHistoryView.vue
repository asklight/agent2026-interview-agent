<template>
  <main class="page-frame training-history-page">
    <header class="workspace-heading">
      <div><p class="page-kicker">TRAINING HISTORY</p><h1>训练历史</h1></div>
      <p>八股、项目深挖和算法口述统一收在这里。隐藏记录不会删除原始对话和复盘证据。</p>
    </header>
    <div class="history-filter-row">
      <div class="algorithm-filters">
        <button v-for="item in types" :key="item.value" type="button" :class="{ active: type === item.value }" @click="setType(item.value)">{{ item.label }}</button>
      </div>
      <el-select v-model="status" aria-label="状态筛选" @change="load(1)">
        <el-option label="全部状态" value="" /><el-option label="进行中" value="IN_PROGRESS" /><el-option label="已完成" value="FINISHED" />
      </el-select>
    </div>
    <section v-if="loading" class="algorithm-library__state">正在同步训练记录...</section>
    <section v-else-if="error" class="algorithm-library__state"><p>{{ error }}</p><el-button @click="load(page)">重新加载</el-button></section>
    <section v-else-if="!items.length" class="algorithm-library__state"><h2>还没有训练记录</h2><p>完成一次练习后，它会自动出现在这里。</p></section>
    <section v-else class="history-list">
      <article v-for="item in items" :key="item.id">
        <span class="history-type">{{ typeName(item.trainingType) }}</span>
        <div><h2>{{ item.title }}</h2><p>{{ item.summary }}</p><small>{{ formatTime(item.startedAt) }} · {{ item.status === 'FINISHED' ? '已完成' : '进行中' }}</small></div>
        <div class="history-actions">
          <RouterLink class="primary-link" :to="target(item)">{{ item.status === 'FINISHED' ? '查看复盘' : '继续训练' }}</RouterLink>
          <el-tooltip content="隐藏记录" placement="top"><button type="button" aria-label="隐藏记录" @click="hide(item.id)"><Delete /></button></el-tooltip>
        </div>
      </article>
    </section>
    <el-pagination v-if="total > pageSize" layout="prev, pager, next" :total="total" :page-size="pageSize" :current-page="page" @current-change="load" />
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Delete } from '@element-plus/icons-vue'
import { getTrainingHistory, hideTrainingHistory } from '../api/trainingHistoryApi'
import type { TrainingHistoryItem, TrainingType } from '../model/types'

const items = ref<TrainingHistoryItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const type = ref('')
const status = ref('')
const loading = ref(false)
const error = ref('')
const types = [{ value: '', label: '全部' }, { value: 'KNOWLEDGE', label: '八股' }, { value: 'PROJECT_DEEP_DIVE', label: '项目' }, { value: 'ALGORITHM', label: '算法' }]
onMounted(() => load(1))

async function load(nextPage: number) {
  loading.value = true; error.value = ''; page.value = nextPage
  try {
    const result = (await getTrainingHistory({ type: type.value || undefined, status: status.value || undefined, page: nextPage, pageSize })).data.data
    items.value = result.items; total.value = result.total
  } catch { error.value = '训练记录暂时无法同步，请稍后重试。' }
  finally { loading.value = false }
}
function setType(value: string) { type.value = value; load(1) }
async function hide(id: number) { await hideTrainingHistory(id); await load(page.value) }
function typeName(value: TrainingType) { return { KNOWLEDGE: '八股练习', PROJECT_DEEP_DIVE: '项目深挖', ALGORITHM: '算法口述' }[value] }
function formatTime(value: string) { return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value)) }
function target(item: TrainingHistoryItem) {
  if (item.trainingType === 'ALGORITHM') return item.status === 'FINISHED' ? `/practice/algorithm/${item.sourceSessionId}/report` : `/algorithm-interview/${item.sourceSessionId}`
  if (item.trainingType === 'PROJECT_DEEP_DIVE') return item.status === 'FINISHED' ? `/interview/${item.sourceSessionId}/report` : `/interview/${item.sourceSessionId}`
  return '/practice/knowledge'
}
</script>
