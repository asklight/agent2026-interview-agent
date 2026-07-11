import { createRouter, createWebHistory } from 'vue-router'
import AppShell from '@/layouts/AppShell.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AppShell,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
        },
        {
          path: 'project-deep-dive',
          name: 'project-deep-dive-home',
          component: () => import('@/features/project-deep-dive/views/ProjectDeepDiveHome.vue'),
        },
        {
          path: 'project-deep-dive/new',
          name: 'project-deep-dive-new',
          component: () => import('@/features/project-deep-dive/views/ProjectSetup.vue'),
        },
        {
          path: 'project-deep-dive/:profileId(\\d+)',
          name: 'project-deep-dive-profile',
          component: () => import('@/features/project-deep-dive/views/ProjectSetup.vue'),
          props: true,
        },
      ],
    },
    {
      path: '/practice/knowledge',
      name: 'knowledge-practice',
      component: () => import('@/features/knowledge-practice/views/KnowledgePracticeView.vue'),
    },
    {
      path: '/interview/:sessionId(\\d+)',
      name: 'project-interview-room',
      component: () => import('@/features/project-deep-dive/views/InterviewRoom.vue'),
      props: true,
    },
    {
      path: '/interview/:sessionId(\\d+)/report',
      name: 'project-interview-report',
      component: () => import('@/features/project-deep-dive/views/InterviewReport.vue'),
      props: true,
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
