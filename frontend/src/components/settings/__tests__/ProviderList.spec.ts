import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { ProviderView } from '../../../api/providers'
import ProviderList from '../ProviderList.vue'

const provider: ProviderView = { id: 7, displayName: '视觉模型', presetCode: 'QWEN', protocol: 'OPENAI_CHAT_COMPLETIONS', baseUrl: 'https://example.com/v1', model: 'model-id', supportsText: true, supportsVision: true, enabled: true, keyConfigured: true, persistenceAvailable: true }

describe('ProviderList', () => {
  it('shows provider state and requires a second explicit action before deleting a key', async () => {
    const wrapper = mount(ProviderList, { props: { providers: [provider], routing: { defaultTextProviderId: 7, defaultVisionProviderId: 7, dailyLimit: 50, maxOutputTokens: 1024 }, probes: { 7: { kind: 'success', model: 'model-id', latencyMs: 83 } } } })

    expect(wrapper.text()).toContain('文本、视觉')
    expect(wrapper.text()).toContain('密钥已配置')
    expect(wrapper.text()).toContain('可持久化')
    expect(wrapper.text()).toContain('83 ms')
    await wrapper.get('[aria-label="删除 视觉模型 的密钥"]').trigger('click')
    expect(wrapper.emitted('clear-key')).toBeUndefined()
    await wrapper.get('[aria-label="确认删除 视觉模型 的密钥"]').trigger('click')
    expect(wrapper.emitted('clear-key')).toEqual([[7]])
  })

  it('limits default selectors to enabled providers with the requested capability', () => {
    const wrapper = mount(ProviderList, { props: { providers: [provider, { ...provider, id: 8, displayName: '停用', supportsVision: false, enabled: false }], routing: { defaultTextProviderId: 7, defaultVisionProviderId: 7, dailyLimit: 50, maxOutputTokens: 1024 }, probes: {} } })
    expect(wrapper.get('[aria-label="默认文本模型"]').text()).toContain('视觉模型')
    expect(wrapper.get('[aria-label="默认视觉模型"]').text()).not.toContain('停用')
  })
})
