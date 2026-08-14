import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import type { ProviderView } from '../../../api/providers'
import ProviderForm from '../ProviderForm.vue'

const api = vi.hoisted(() => ({ createProvider: vi.fn(), updateProvider: vi.fn() }))
vi.mock('../../../api/providers', async (importOriginal) => ({ ...await importOriginal<typeof import('../../../api/providers')>(), ...api }))

const presets = ['QWEN', 'CUSTOM'] as const
const baseProvider: ProviderView = { id: 3, displayName: '已配置模型', presetCode: 'CUSTOM', protocol: 'OPENAI_CHAT_COMPLETIONS', baseUrl: 'https://custom.example/v1', model: 'exact-model', supportsText: true, supportsVision: false, enabled: true, keyConfigured: true, persistenceAvailable: true }

describe('ProviderForm', () => {
  it('only fills untouched preset fields and never invents a model name', async () => {
    const wrapper = mount(ProviderForm, { props: { presets, onSaved: vi.fn() } })
    const inputs = wrapper.findAll('input')
    await inputs[1].setValue('https://mine.example/v1')
    await wrapper.get('select').setValue('QWEN')

    expect((inputs[0].element as HTMLInputElement).value).toContain('阿里云百炼')
    expect((inputs[1].element as HTMLInputElement).value).toBe('https://mine.example/v1')
    expect((inputs[2].element as HTMLInputElement).value).toBe('')
  })

  it('warns for a custom URL and clears the password input after a rejected save', async () => {
    api.updateProvider.mockRejectedValue(new Error('failed'))
    const wrapper = mount(ProviderForm, { props: { provider: baseProvider, presets, onSaved: vi.fn() } })
    const password = wrapper.get('input[type="password"]')
    await password.setValue('sk-local-secret')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('自定义地址')
    expect((password.element as HTMLInputElement).value).toBe('')
  })

  it('submits the local password directly and only reports the secret-free provider view', async () => {
    const onSaved = vi.fn()
    api.createProvider.mockResolvedValue({ ...baseProvider, id: 8 })
    const wrapper = mount(ProviderForm, { props: { presets, onSaved } })
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('本地服务')
    await inputs[1].setValue('https://api.example.com/v1')
    await inputs[2].setValue('model-id')
    const password = wrapper.get('input[type="password"]')
    await password.setValue('sk-local-secret')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(api.createProvider).toHaveBeenCalledWith(expect.objectContaining({ model: 'model-id' }), 'sk-local-secret')
    expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ id: 8, model: 'exact-model' }))
    expect((password.element as HTMLInputElement).value).toBe('')
  })
})
