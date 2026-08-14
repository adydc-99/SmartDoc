import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import type { ProviderView } from '../../api/providers'
import { useAiProvidersStore } from '../aiProviders'

const api = vi.hoisted(() => ({
  getProviderPresets: vi.fn(), listProviders: vi.fn(), getRouting: vi.fn(),
  deleteProviderKey: vi.fn(), deleteProvider: vi.fn(), testProvider: vi.fn(), updateRouting: vi.fn(),
}))
vi.mock('../../api/providers', () => api)

const provider: ProviderView = { id: 7, displayName: 'Qwen', presetCode: 'QWEN', protocol: 'OPENAI_CHAT_COMPLETIONS', baseUrl: 'https://example.com/v1', model: 'qwen-local', supportsText: true, supportsVision: true, enabled: true, keyConfigured: true, persistenceAvailable: false }

describe('AI provider store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear(); sessionStorage.clear()
    api.getProviderPresets.mockResolvedValue(['QWEN'])
    api.listProviders.mockResolvedValue([provider])
    api.getRouting.mockResolvedValue({ defaultTextProviderId: 7, defaultVisionProviderId: 7, dailyLimit: 50, maxOutputTokens: 1024 })
  })

  it('loads presets, providers, and routing together without putting an API key in state', async () => {
    const store = useAiProvidersStore()
    await store.load()

    expect(api.getProviderPresets).toHaveBeenCalledOnce()
    expect(api.listProviders).toHaveBeenCalledOnce()
    expect(api.getRouting).toHaveBeenCalledOnce()
    expect(JSON.stringify(store.$state)).not.toContain('apiKey')
    expect(JSON.stringify(store.$state)).not.toContain('sk-local-secret')
  })

  it('accepts provider views without exposing a key-handling save action', () => {
    const store = useAiProvidersStore()
    store.upsert(provider)

    expect(store.providers).toEqual([provider])
    expect(store).not.toHaveProperty('save')
  })
})
