import { defineComponent, onMounted, onUnmounted } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import App from '@/App.vue'
import productionRouter from '@/router'

describe('route lifecycle isolation', () => {
  it('remounts path-sensitive pages when a route parameter changes', async () => {
    let mounts = 0
    let unmounts = 0
    const Probe = defineComponent({
      props: { sessionId: { type: String, required: true } },
      setup() {
        onMounted(() => { mounts++ })
        onUnmounted(() => { unmounts++ })
      },
      template: '<div>{{ sessionId }}</div>',
    })
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{
        path: '/interview/:sessionId',
        component: Probe,
        props: true,
        meta: { remountOnPathChange: true },
      }],
    })
    await router.push('/interview/7')
    const wrapper = mount(App, { global: { plugins: [router] } })
    await router.isReady()
    await flushPromises()

    await router.push('/interview/8')
    await flushPromises()

    expect(wrapper.text()).toBe('8')
    expect(mounts).toBe(2)
    expect(unmounts).toBe(1)
    wrapper.unmount()
  })

  it('marks both session-bound production routes as path-sensitive', () => {
    const routes = productionRouter.getRoutes()

    expect(routes.find(route => route.name === 'project-interview-room')?.meta.remountOnPathChange).toBe(true)
    expect(routes.find(route => route.name === 'project-interview-report')?.meta.remountOnPathChange).toBe(true)
  })

  it('passes the project recommendation dimension from query parameters into setup props', () => {
    const routes = productionRouter.getRoutes()
    const profileRoute = routes.find(route => route.name === 'project-deep-dive-profile')
    const newRoute = routes.find(route => route.name === 'project-deep-dive-new')
    const profileProps = profileRoute?.props.default
    const newProps = newRoute?.props.default
    type ProjectSetupRoute = {
      params: { profileId: string }
      query: { targetDimension: string }
    }
    const route: ProjectSetupRoute = {
      params: { profileId: '27' },
      query: { targetDimension: 'PROJECT.TRADEOFF' },
    }

    expect(typeof profileProps).toBe('function')
    expect(typeof newProps).toBe('function')
    expect((profileProps as (route: ProjectSetupRoute) => object)(route)).toEqual({
      profileId: '27',
      targetDimension: 'PROJECT.TRADEOFF',
    })
    expect((newProps as (route: ProjectSetupRoute) => object)(route)).toEqual({
      targetDimension: 'PROJECT.TRADEOFF',
    })
  })
})
