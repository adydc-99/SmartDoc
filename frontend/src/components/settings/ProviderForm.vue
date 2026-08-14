<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { createProvider, PROVIDER_PRESETS, PROVIDER_PROTOCOL, type ProviderDraft, type ProviderPresetCode, type ProviderView, updateProvider } from '../../api/providers'

const props = defineProps<{
  provider?: ProviderView
  presets: readonly ProviderPresetCode[]
  saving?: boolean
  onSaved: (provider: ProviderView) => void
}>()
const apiKey = ref('')
const status = ref('')
const submitting = ref(false)
const touched = reactive({ displayName: false, baseUrl: false, supportsText: false, supportsVision: false })
const blank = (): ProviderDraft => ({ displayName: '', presetCode: 'CUSTOM', protocol: PROVIDER_PROTOCOL, baseUrl: '', model: '', supportsText: true, supportsVision: false, enabled: true })
const draft = reactive<ProviderDraft>(blank())
const preset = computed(() => PROVIDER_PRESETS[draft.presetCode])
const customUrlWarning = computed(() => draft.presetCode === 'CUSTOM' || (preset.value.baseUrl !== '' && draft.baseUrl !== preset.value.baseUrl))

function reset(provider?: ProviderView) {
  Object.assign(draft, provider ? {
    displayName: provider.displayName, presetCode: provider.presetCode, protocol: provider.protocol, baseUrl: provider.baseUrl, model: provider.model,
    supportsText: provider.supportsText, supportsVision: provider.supportsVision, enabled: provider.enabled,
  } : blank())
  Object.assign(touched, { displayName: false, baseUrl: false, supportsText: false, supportsVision: false })
  apiKey.value = ''; status.value = ''
}
watch(() => props.provider, reset, { immediate: true })
function applyPreset() {
  const selected = PROVIDER_PRESETS[draft.presetCode]
  if (!touched.displayName) draft.displayName = selected.label
  if (!touched.baseUrl) draft.baseUrl = selected.baseUrl
  if (!touched.supportsText) draft.supportsText = selected.supportsText
  if (!touched.supportsVision) draft.supportsVision = selected.supportsVision
}
async function submit() {
  status.value = ''; submitting.value = true
  try {
    const saved = props.provider ? await updateProvider(props.provider.id, { ...draft }, apiKey.value) : await createProvider({ ...draft }, apiKey.value)
    props.onSaved(saved); status.value = '服务商已保存，密钥输入已清除。'
  }
  catch { status.value = '保存失败，请检查地址、模型和密钥。' }
  finally { apiKey.value = ''; submitting.value = false }
}
</script>
<template>
  <form class="provider-form" @submit.prevent="submit">
    <div class="provider-form-grid">
      <label>预设服务商
        <select v-model="draft.presetCode" aria-label="预设服务商" @change="applyPreset">
          <option v-for="code in presets" :key="code" :value="code">{{ PROVIDER_PRESETS[code].label }}</option>
        </select>
      </label>
      <label>显示名称
        <input v-model="draft.displayName" required maxlength="80" @input="touched.displayName = true">
      </label>
      <label>Base URL
        <input v-model="draft.baseUrl" type="url" required inputmode="url" @input="touched.baseUrl = true">
      </label>
      <label>模型
        <input v-model="draft.model" required maxlength="120" autocomplete="off">
      </label>
    </div>
    <p v-if="customUrlWarning" class="inline-notice" role="status">自定义地址将由服务端进行安全校验，请仅使用可信的兼容服务地址。</p>
    <fieldset class="provider-capabilities"><legend>能力</legend><label><input v-model="draft.supportsText" type="checkbox" @change="touched.supportsText = true">文本</label><label><input v-model="draft.supportsVision" type="checkbox" @change="touched.supportsVision = true">视觉</label><label><input v-model="draft.enabled" type="checkbox">启用此服务商</label></fieldset>
    <label>API 密钥
      <input v-model="apiKey" type="password" autocomplete="new-password" :placeholder="provider?.keyConfigured ? '已配置；留空则保持不变' : '仅用于本次提交'">
    </label>
    <p class="muted">密钥只在提交时发送，不会写入浏览器存储或 URL。</p>
    <div class="provider-form-actions"><button class="button primary" :disabled="saving || submitting">{{ saving || submitting ? '保存中…' : '保存服务商' }}</button></div>
    <p class="form-status" aria-live="polite">{{ status }}</p>
  </form>
</template>
