import { createRouter, createWebHistory } from 'vue-router'
import AppShell from '@/layouts/AppShell.vue'
import { useAuthStore } from '@/features/identity/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/features/identity/views/LoginView.vue'),
      meta: { publicOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/features/identity/views/RegisterView.vue'),
      meta: { publicOnly: true },
    },
    {
      path: '/',
      component: AppShell,
      meta: { requiresAuth: true },
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
        {
          path: 'practice/knowledge',
          name: 'knowledge-practice',
          component: () => import('@/features/knowledge-practice/views/KnowledgePracticeView.vue'),
        },
        {
          path: 'practice/algorithm',
          name: 'algorithm-selection',
          component: () => import('@/features/algorithm-practice/views/AlgorithmSelectionView.vue'),
        },
        {
          path: 'practice/algorithm/:sessionId(\\d+)/report',
          name: 'algorithm-report',
          component: () => import('@/features/algorithm-practice/views/AlgorithmReportView.vue'),
          props: true,
          meta: { remountOnPathChange: true },
        },
      ],
    },
    {
      path: '/interview/:sessionId(\\d+)',
      name: 'project-interview-room',
      component: () => import('@/features/project-deep-dive/views/InterviewRoom.vue'),
      props: true,
      meta: { remountOnPathChange: true, requiresAuth: true },
    },
    {
      path: '/interview/:sessionId(\\d+)/report',
      name: 'project-interview-report',
      component: () => import('@/features/project-deep-dive/views/InterviewReport.vue'),
      props: true,
      meta: { remountOnPathChange: true, requiresAuth: true },
    },
    {
      path: '/algorithm-interview/:sessionId(\\d+)',
      name: 'algorithm-room',
      component: () => import('@/features/algorithm-practice/views/AlgorithmRoomView.vue'),
      props: true,
      meta: { remountOnPathChange: true, requiresAuth: true },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.initialize()
  if (to.matched.some(record => record.meta.requiresAuth) && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.matched.some(record => record.meta.publicOnly) && auth.isAuthenticated) {
    const redirect = typeof to.query.redirect === 'string' && to.query.redirect.startsWith('/')
      ? to.query.redirect : '/'
    return redirect
  }
  return true
})

export default router
