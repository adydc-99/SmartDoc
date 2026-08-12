import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import AppSidebar from '../AppSidebar.vue'

describe('AppSidebar', () => {
  it('exposes labeled navigation and a mobile drawer trigger', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div />' } }],
    })
    await router.push('/')
    await router.isReady()
    const wrapper = mount(AppSidebar, { global: { plugins: [router] } })

    expect(wrapper.get('nav[aria-label="主导航"]')).toBeTruthy()
    expect(wrapper.get('button[aria-label="打开资料导航"]')).toBeTruthy()
  })
})
