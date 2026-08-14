<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ProviderView, RoutingConfig } from '../../api/providers'
import type { ProbeState } from '../../stores/aiProviders'

const props = defineProps<{ providers: ProviderView[]; routing?: RoutingConfig; probes: Record<number, ProbeState>; busy?: boolean }>()
const emit = defineEmits<{ edit: [provider: ProviderView]; test: [id: number]; remove: [id: number]; 'clear-key': [id: number]; routing: [routing: RoutingConfig] }>()
const confirmingKeyId = ref<number | undefined>()
const textProviders = computed(() => props.providers.filter((provider) => provider.enabled && provider.supportsText))
const visionProviders = computed(() => props.providers.filter((provider) => provider.enabled && provider.supportsVision))
const capability = (provider: ProviderView) => provider.supportsText && provider.supportsVision ? '文本、视觉' : provider.supportsText ? '文本' : '视觉'
const probe = (id: number) => {
  const state = props.probes[id]
  if (!state) return undefined
  if (state.kind === 'success') return { kind: 'success', text: `连接成功：${state.model} · ${state.latencyMs} ms` }
  if (state.kind === 'error') return { kind: 'error', text: `连接失败：${state.message}` }
  return { kind: 'loading', text: '正在测试连接…' }
}
function updateRoute(key: 'defaultTextProviderId' | 'defaultVisionProviderId', event: Event) {
  if (!props.routing) return
  const value = (event.target as HTMLSelectElement).value
  emit('routing', { ...props.routing, [key]: value ? Number(value) : null })
}
</script>
<template>
  <section class="provider-list" aria-label="已配置的 AI 服务商">
    <div v-if="routing" class="provider-routing">
      <label>默认文本模型<select aria-label="默认文本模型" :value="routing.defaultTextProviderId ?? ''" @change="updateRoute('defaultTextProviderId', $event)"><option value="">未选择</option><option v-for="provider in textProviders" :key="provider.id" :value="provider.id">{{ provider.displayName }}</option></select></label>
      <label>默认视觉模型<select aria-label="默认视觉模型" :value="routing.defaultVisionProviderId ?? ''" @change="updateRoute('defaultVisionProviderId', $event)"><option value="">未选择</option><option v-for="provider in visionProviders" :key="provider.id" :value="provider.id">{{ provider.displayName }}</option></select></label>
    </div>
    <p v-if="!providers.length" class="muted provider-empty">还没有服务商。请先添加一个。</p>
    <article v-for="provider in providers" :key="provider.id" class="provider-card">
      <header><div><h3>{{ provider.displayName }}</h3><p>{{ provider.model }} · {{ capability(provider) }}</p></div><span :class="['status-pill', { disabled: !provider.enabled }]">{{ provider.enabled ? '已启用' : '已停用' }}</span></header>
      <dl><div><dt>密钥</dt><dd>{{ provider.keyConfigured ? '密钥已配置' : '未配置密钥' }}</dd></div><div><dt>保存方式</dt><dd>{{ provider.persistenceAvailable ? '可持久化' : '仅后端进程内存' }}</dd></div><div><dt>地址</dt><dd>{{ provider.baseUrl }}</dd></div></dl>
      <p v-if="probe(provider.id)" :class="['probe-result', probe(provider.id)?.kind]" aria-live="polite">{{ probe(provider.id)?.text }}</p>
      <div class="provider-card-actions"><button class="button secondary" :disabled="busy" :aria-label="`测试 ${provider.displayName}`" @click="emit('test', provider.id)">测试</button><button class="button secondary" :disabled="busy" :aria-label="`编辑 ${provider.displayName}`" @click="emit('edit', provider)">编辑</button><button v-if="confirmingKeyId !== provider.id" class="button secondary" :disabled="busy || !provider.keyConfigured" :aria-label="`删除 ${provider.displayName} 的密钥`" @click="confirmingKeyId = provider.id">删除密钥</button><button v-else class="button danger" :disabled="busy" :aria-label="`确认删除 ${provider.displayName} 的密钥`" @click="emit('clear-key', provider.id); confirmingKeyId = undefined">确认删除密钥</button><button class="button secondary danger-text" :disabled="busy" :aria-label="`删除 ${provider.displayName}`" @click="emit('remove', provider.id)">删除服务商</button></div>
    </article>
  </section>
</template>
