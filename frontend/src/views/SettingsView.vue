<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import ProviderForm from '../components/settings/ProviderForm.vue'
import ProviderList from '../components/settings/ProviderList.vue'
import type { ProviderView, RoutingConfig } from '../api/providers'
import { useAiProvidersStore } from '../stores/aiProviders'

const store = useAiProvidersStore()
const { presets, providers, routing, probes, loading, busy, status } = storeToRefs(store)
const editing = ref<ProviderView | undefined>()
const formTitle = computed(() => editing.value ? `编辑 ${editing.value.displayName}` : '添加服务商')
onMounted(() => void store.load())
function saveProvider(provider: ProviderView) {
  store.upsert(provider)
  editing.value = undefined
}
async function saveRouting(value: RoutingConfig) { await store.saveRouting(value) }
</script>
<template>
  <div class="page settings-page">
    <header class="page-heading"><div><p class="eyebrow">SETTINGS</p><h1>AI 服务商</h1><p>为文本与视觉任务配置兼容服务，并选择各自的默认模型。</p></div><button class="button primary" @click="editing = undefined">添加服务商</button></header>
    <p class="security-note">密钥提交后会立即从浏览器内存和输入框清除，绝不会写入 localStorage、sessionStorage 或 URL。仅当后端环境支持安全持久化时，服务商卡片才会显示“可持久化”。</p>
    <section class="settings-grid">
      <section class="panel provider-editor" :aria-label="formTitle"><header><div><p class="eyebrow">PROVIDER</p><h2>{{ formTitle }}</h2></div></header><ProviderForm :key="editing?.id ?? 'new'" :provider="editing" :presets="presets" :saving="busy" :on-saved="saveProvider" /></section>
      <section class="panel provider-management"><header><div><p class="eyebrow">ROUTING</p><h2>服务商与默认路由</h2></div><span v-if="loading" class="muted">正在加载…</span></header><ProviderList :providers="providers" :routing="routing" :probes="probes" :busy="busy" @edit="editing = $event" @test="store.probe" @clear-key="store.clearKey" @remove="store.remove" @routing="saveRouting" /></section>
    </section>
    <p class="form-status" aria-live="polite">{{ status }}</p>
  </div>
</template>
