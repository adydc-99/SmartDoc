import { defineStore } from 'pinia'
import * as providersApi from '../api/providers'
import type { ProviderView, RoutingConfig } from '../api/providers'

export type ProbeState = { kind: 'success'; model: string; latencyMs: number } | { kind: 'error'; message: string } | { kind: 'loading' }

export const useAiProvidersStore = defineStore('ai-providers', {
  state: () => ({
    presets: [] as providersApi.ProviderPresetCode[], providers: [] as ProviderView[], routing: undefined as RoutingConfig | undefined,
    probes: {} as Record<number, ProbeState>, loading: false, busy: false, status: '',
  }),
  actions: {
    async load() {
      this.loading = true; this.status = ''
      try {
        const [presets, providers, routing] = await Promise.all([providersApi.getProviderPresets(), providersApi.listProviders(), providersApi.getRouting()])
        this.presets = presets; this.providers = providers; this.routing = routing
      } catch (error) { this.status = providersApi.normalizedProviderError(error) }
      finally { this.loading = false }
    },
    upsert(provider: ProviderView) {
      const index = this.providers.findIndex((candidate) => candidate.id === provider.id)
      if (index === -1) this.providers.push(provider); else this.providers.splice(index, 1, provider)
      this.status = '服务商已保存。'
    },
    async clearKey(id: number) {
      this.busy = true
      try { await providersApi.deleteProviderKey(id); await this.load(); this.status = '密钥已删除。' }
      catch (error) { this.status = providersApi.normalizedProviderError(error) }
      finally { this.busy = false }
    },
    async remove(id: number) {
      this.busy = true
      try { await providersApi.deleteProvider(id); this.providers = this.providers.filter((provider) => provider.id !== id); this.status = '服务商已删除。' }
      catch (error) { this.status = providersApi.normalizedProviderError(error) }
      finally { this.busy = false }
    },
    async probe(id: number) {
      this.probes[id] = { kind: 'loading' }
      try { const result = await providersApi.testProvider(id); this.probes[id] = { kind: 'success', model: result.model, latencyMs: result.latencyMs } }
      catch (error) { this.probes[id] = { kind: 'error', message: providersApi.normalizedProviderError(error) } }
    },
    async saveRouting(routing: RoutingConfig) {
      this.busy = true
      try { this.routing = await providersApi.updateRouting(routing); this.status = '默认服务商已更新。' }
      catch (error) { this.status = providersApi.normalizedProviderError(error) }
      finally { this.busy = false }
    },
  },
})
