import { api, messageOf } from '../api'

export const PROVIDER_PROTOCOL = 'OPENAI_CHAT_COMPLETIONS' as const
export const PROVIDER_PRESETS = {
  DEEPSEEK: { label: 'DeepSeek', baseUrl: 'https://api.deepseek.com/v1', supportsText: true, supportsVision: false },
  QWEN: { label: '阿里云百炼', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', supportsText: true, supportsVision: true },
  VOLCENGINE: { label: '火山方舟', baseUrl: 'https://ark.cn-beijing.volces.com/api/v3', supportsText: true, supportsVision: true },
  ZHIPU: { label: '智谱', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', supportsText: true, supportsVision: false },
  HUNYUAN: { label: '腾讯混元', baseUrl: 'https://api.hunyuan.cloud.tencent.com/v1', supportsText: true, supportsVision: true },
  QIANFAN: { label: '百度千帆', baseUrl: 'https://qianfan.baidubce.com/v3', supportsText: true, supportsVision: false },
  SILICONFLOW: { label: '硅基流动', baseUrl: 'https://api.siliconflow.cn/v1', supportsText: true, supportsVision: false },
  CUSTOM: { label: '自定义兼容服务', baseUrl: '', supportsText: true, supportsVision: false },
} as const

export type ProviderPresetCode = keyof typeof PROVIDER_PRESETS
export interface ProviderView {
  id: number
  displayName: string
  presetCode: ProviderPresetCode
  protocol: typeof PROVIDER_PROTOCOL
  baseUrl: string
  model: string
  supportsText: boolean
  supportsVision: boolean
  enabled: boolean
  keyConfigured: boolean
  persistenceAvailable: boolean
  maskedKey?: string
  createdAt?: string
  updatedAt?: string
}
export interface ProviderDraft {
  displayName: string
  presetCode: ProviderPresetCode
  protocol: typeof PROVIDER_PROTOCOL
  baseUrl: string
  model: string
  supportsText: boolean
  supportsVision: boolean
  enabled: boolean
}
export interface RoutingConfig {
  defaultTextProviderId: number | null
  defaultVisionProviderId: number | null
  dailyLimit: number
  maxOutputTokens: number
}
export interface ProbeResult { success: boolean; providerId: number; model: string; latencyMs: number }

export const getProviderPresets = () => api.get<ProviderPresetCode[]>('/ai/provider-presets').then((response) => response.data)
export const listProviders = () => api.get<ProviderView[]>('/ai/providers').then((response) => response.data)
export const getRouting = () => api.get<RoutingConfig>('/ai/routing').then((response) => response.data)
const requestBody = (draft: ProviderDraft, apiKey: string) => ({ ...draft, apiKey })
export const createProvider = (draft: ProviderDraft, apiKey: string) => api.post<ProviderView>('/ai/providers', requestBody(draft, apiKey)).then((response) => response.data)
export const updateProvider = (id: number, draft: ProviderDraft, apiKey: string) => api.put<ProviderView>(`/ai/providers/${id}`, requestBody(draft, apiKey)).then((response) => response.data)
export const deleteProviderKey = (id: number) => api.delete(`/ai/providers/${id}/key`)
export const deleteProvider = (id: number) => api.delete(`/ai/providers/${id}`)
export const testProvider = (id: number) => api.post<ProbeResult>(`/ai/providers/${id}/test`).then((response) => response.data)
export const updateRouting = (routing: RoutingConfig) => api.put<RoutingConfig>('/ai/routing', routing).then((response) => response.data)
export const normalizedProviderError = (error: unknown) => messageOf(error) || '连接失败，请检查地址、模型和密钥。'

export type VisionAction = 'DIRECT' | 'DEEP_ANALYSIS'
export interface VisualRoutingProvider { id: number; model: string }
export interface VisualProviderState { visionProvider: VisualRoutingProvider | null; textProvider: VisualRoutingProvider | null }
export interface VisionObservation { description: string; ocrText: string; codeOrDiagram: string; uncertainties: string[] }
export interface VisionActionResponse {
  action: VisionAction
  observation: VisionObservation
  analysis?: string
  visionModel: string
  textModel?: string
  visionCacheHit: boolean
}

export const getVisualProviderState = async (): Promise<VisualProviderState> => {
  const [providers, routing] = await Promise.all([listProviders(), getRouting()])
  const vision = providers.find((provider) => provider.id === routing.defaultVisionProviderId
    && provider.enabled && provider.supportsVision && provider.keyConfigured)
  const text = providers.find((provider) => provider.id === routing.defaultTextProviderId
    && provider.enabled && provider.supportsText && provider.keyConfigured)
  return {
    visionProvider: vision ? { id: vision.id, model: vision.model } : null,
    textProvider: text ? { id: text.id, model: text.model } : null,
  }
}

export const runVisionAction = (documentId: number, form: FormData) =>
  api.post<VisionActionResponse>(`/documents/${documentId}/vision-actions`, form).then((response) => response.data)
