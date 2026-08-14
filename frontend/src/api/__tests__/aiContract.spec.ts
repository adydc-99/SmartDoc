import {describe,expect,it,vi} from 'vitest'
import {AI_ACTION_OPTIONS,BACKEND_AI_ACTIONS} from '../study'
import {createProvider,listProviders,type ProviderDraft} from '../providers'

const request = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))
vi.mock('../../api', () => ({ api: request }))

describe('AI action backend contract',()=>{
  it('uses only exact backend AiAction enum values for every visible action',()=>{
    expect(BACKEND_AI_ACTIONS).toEqual(['ASK','DOCUMENT_SUMMARY','CURRENT_PAGE_SUMMARY','EXPLAIN','SUMMARIZE','EXPLAIN_CODE','LINE_BY_LINE','COMPLEXITY','FIND_ISSUES','GENERATE_EXAMPLE','INTERVIEW_QUESTION'])
    expect(AI_ACTION_OPTIONS.map(option=>option.value)).toEqual(['ASK','DOCUMENT_SUMMARY','CURRENT_PAGE_SUMMARY','EXPLAIN','SUMMARIZE','EXPLAIN_CODE'])
    expect(AI_ACTION_OPTIONS.every(option=>BACKEND_AI_ACTIONS.includes(option.value))).toBe(true)
  })
})

describe('provider backend contract', () => {
  it('uses the provider collection endpoint and sends a secret only in the JSON body', async () => {
    request.get.mockResolvedValue({ data: [] })
    request.post.mockResolvedValue({ data: { id: 4 } })
    const draft: ProviderDraft = {
      displayName: '我的模型', presetCode: 'CUSTOM', protocol: 'OPENAI_CHAT_COMPLETIONS',
      baseUrl: 'https://api.example.com/v1', model: 'model-id', supportsText: true, supportsVision: false, enabled: true,
    }

    await listProviders()
    await createProvider(draft, 'sk-local-secret')

    expect(request.get).toHaveBeenCalledWith('/ai/providers')
    expect(request.post).toHaveBeenCalledWith('/ai/providers', { ...draft, apiKey: 'sk-local-secret' })
    expect(request.post.mock.calls[0][1]).not.toHaveProperty('params')
  })
})
