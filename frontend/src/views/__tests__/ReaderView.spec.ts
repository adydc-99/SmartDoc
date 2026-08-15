import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ReaderView from '../ReaderView.vue'

const readerApi = vi.hoisted(() => ({
  getReaderDocument: vi.fn(), getReaderContent: vi.fn(), getProgress: vi.fn(),
  listDocumentNotes: vi.fn(), saveProgress: vi.fn(), searchDocument: vi.fn(),
}))
const studyApi = vi.hoisted(() => ({
  createNote: vi.fn(), updateNote: vi.fn(), deleteNote: vi.fn(), runAi: vi.fn(),
  AI_ACTION_OPTIONS: [
    { value: 'ASK', label: '提问' }, { value: 'DOCUMENT_SUMMARY', label: '总结全文' },
    { value: 'CURRENT_PAGE_SUMMARY', label: '总结当前页' }, { value: 'EXPLAIN', label: '解释选区' },
    { value: 'SUMMARIZE', label: '总结选区' }, { value: 'EXPLAIN_CODE', label: '解释代码' },
  ],
}))
const providersApi = vi.hoisted(() => ({
  getVisualProviderState: vi.fn(), runVisionAction: vi.fn(),
}))
const routeLeave = vi.hoisted(() => ({ callback: undefined as undefined | (() => void) }))

vi.mock('../../api/reader', () => readerApi)
vi.mock('../../api/study', () => studyApi)
vi.mock('../../api/providers', () => providersApi)
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '7' }, query: {} }),
  onBeforeRouteLeave: (callback: () => void) => { routeLeave.callback = callback },
}))

describe('ReaderView', () => {
  beforeEach(() => {
    vi.clearAllMocks(); vi.useRealTimers()
    readerApi.getReaderDocument.mockResolvedValue({ id: 7, name: 'guide.txt', documentType: 'TXT', pageCount: null, status: 'READY' })
    readerApi.getReaderContent.mockResolvedValue({ type: 'text', language: 'text', content: 'Readable content' })
    readerApi.getProgress.mockResolvedValue({ pageNumber: 1, scrollRatio: 0.42, zoom: 1.25, updatedAt: '2026-08-12T12:00:00' })
    readerApi.listDocumentNotes.mockResolvedValue([]); readerApi.saveProgress.mockResolvedValue({})
    providersApi.getVisualProviderState.mockResolvedValue({ visionProvider: null, textProvider: null })
    providersApi.runVisionAction.mockResolvedValue({ action: 'DIRECT', observation: { description: '图表', ocrText: '文字', codeOrDiagram: 'A → B', uncertainties: [] }, visionModel: 'qwen-vl-max', visionCacheHit: false })
  })

  it('restores reading progress and retains the mobile note drawer action', async () => {
    const wrapper = mount(ReaderView, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' }, ReaderPdf: true } } })
    await flushPromises()
    expect(readerApi.getReaderDocument).toHaveBeenCalledWith(7)
    expect(readerApi.getReaderContent).toHaveBeenCalledWith(7, 'TXT')
    expect((wrapper.get('[aria-label="缩放比例"]').element as HTMLInputElement).value).toBe('125')
    expect(wrapper.get('[data-test="reader-scroll"]').attributes('data-restored-ratio')).toBe('0.42')
    expect(wrapper.get('[data-test="mobile-notes-button"]').text()).toContain('笔记')
  })

  it('debounces scroll progress for 800ms and flushes on route leave', async () => {
    vi.useFakeTimers()
    const wrapper = mount(ReaderView, { global: { stubs: { RouterLink: true, ReaderPdf: true } } })
    await flushPromises(); const scroller = wrapper.get('[data-test="reader-scroll"]')
    Object.defineProperties(scroller.element, { scrollTop: { value: 50 }, scrollHeight: { value: 200 }, clientHeight: { value: 100 } })
    await scroller.trigger('scroll'); await vi.advanceTimersByTimeAsync(799)
    expect(readerApi.saveProgress).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(1)
    expect(readerApi.saveProgress).toHaveBeenCalledWith(7, expect.objectContaining({ scrollRatio: 0.5 }))
    await scroller.trigger('scroll'); routeLeave.callback?.()
    expect(readerApi.saveProgress).toHaveBeenCalledTimes(2)
  })

  it('saves PDF page changes immediately and supports explicit AI regeneration', async () => {
    readerApi.getReaderDocument.mockResolvedValue({ id: 7, name: 'guide.pdf', documentType: 'PDF', pageCount: 3, status: 'READY' })
    readerApi.getReaderContent.mockResolvedValue(new Blob(['pdf']))
    studyApi.runAi.mockResolvedValue({ id: 1, action: 'CURRENT_PAGE_SUMMARY', content: 'summary', mode: 'DEMO', cached: true, createdAt: 'now', source: { documentId: 7, pageNumber: 2, text: 'source' } })
    const wrapper = mount(ReaderView, { global: { stubs: { RouterLink: true, ReaderPdf: { template: '<div class="pdf-stub" />', props: ['blob','page','zoom'] } } } })
    await flushPromises(); const next = wrapper.findAll('button').find(button => button.text() === '下一页')!
    await next.trigger('click'); expect(readerApi.saveProgress).toHaveBeenCalledWith(7, expect.objectContaining({ pageNumber: 2 }))
    await wrapper.findAll('button').find(button => button.text() === 'AI')!.trigger('click')
    await wrapper.get('.reader-ai select').setValue('CURRENT_PAGE_SUMMARY'); await wrapper.get('.reader-ai .button.primary').trigger('click'); await flushPromises()
    expect(studyApi.runAi).toHaveBeenLastCalledWith(7, expect.objectContaining({ action: 'CURRENT_PAGE_SUMMARY', force: false }))
    await wrapper.findAll('.ai-answer button').find(button => button.text() === '强制重新生成')!.trigger('click'); await flushPromises()
    expect(studyApi.runAi).toHaveBeenLastCalledWith(7, expect.objectContaining({ force: true }))
  })

  it('sends page 1 when creating a note for a text document', async () => {
    studyApi.createNote.mockResolvedValue({ id: 9, documentId: 7, pageNumber: 1, sourceText: null, contentMarkdown: 'Text note', favorite: false, tags: [] })
    const wrapper = mount(ReaderView, { global: { stubs: { RouterLink: true, ReaderPdf: true } } })
    await flushPromises(); await wrapper.get('[data-test="mobile-notes-button"]').trigger('click')
    await wrapper.findAll('.note-editor textarea')[1].setValue('Text note')
    await wrapper.get('.note-editor .button.primary').trigger('click'); await flushPromises()
    expect(studyApi.createNote).toHaveBeenCalledWith(7, expect.objectContaining({ pageNumber: 1, contentMarkdown: 'Text note' }))
  })

  it('sends the current page when creating a PDF note', async () => {
    readerApi.getReaderDocument.mockResolvedValue({ id: 7, name: 'guide.pdf', documentType: 'PDF', pageCount: 3, status: 'READY' })
    readerApi.getReaderContent.mockResolvedValue(new Blob(['pdf']))
    studyApi.createNote.mockResolvedValue({ id: 10, documentId: 7, pageNumber: 2, sourceText: null, contentMarkdown: 'PDF note', favorite: false, tags: [] })
    const wrapper = mount(ReaderView, { global: { stubs: { RouterLink: true, ReaderPdf: { template: '<div/>', props: ['blob','page','zoom'] } } } })
    await flushPromises(); await wrapper.findAll('button').find(button => button.attributes('aria-label') === '下一页')!.trigger('click')
    await wrapper.get('[data-test="mobile-notes-button"]').trigger('click'); await wrapper.findAll('.note-editor textarea')[1].setValue('PDF note')
    await wrapper.get('.note-editor .button.primary').trigger('click'); await flushPromises()
    expect(studyApi.createNote).toHaveBeenCalledWith(7, expect.objectContaining({ pageNumber: 2, contentMarkdown: 'PDF note' }))
  })

  it('shows visual controls only when owner backend state proves a configured visual provider and discloses exact selected models', async () => {
    const hidden = mount(ReaderView, { global: { stubs: { RouterLink: true, ReaderPdf: true } } })
    await flushPromises(); await hidden.findAll('button').find(button => button.text() === 'AI')!.trigger('click')
    expect(hidden.find('[data-test="vision-actions"]').exists()).toBe(false)
    hidden.unmount()

    providersApi.getVisualProviderState.mockResolvedValue({
      visionProvider: { id: 11, model: 'qwen-vl-max' }, textProvider: { id: 12, model: 'deepseek-chat' },
    })
    const wrapper = mount(ReaderView, { global: { stubs: { RouterLink: true, ReaderPdf: true } } })
    await flushPromises(); await wrapper.findAll('button').find(button => button.text() === 'AI')!.trigger('click')
    const controls=wrapper.get('[data-test="vision-actions"]')
    expect(controls.text()).toContain('识别当前页/图片')
    expect(controls.text()).toContain('视觉模型：qwen-vl-max')
    await controls.get('[aria-label="视觉操作"]').setValue('DEEP_ANALYSIS')
    expect(controls.text()).toContain('视觉识别后深度分析')
    expect(controls.text()).toContain('文本模型：deepseek-chat')
    expect(controls.get('input[type="file"]').attributes('accept')).toContain('image/webp')
  })

  it('submits transient screenshot and current action as FormData then clears blob and revokes preview URL on success', async () => {
    providersApi.getVisualProviderState.mockResolvedValue({ visionProvider: { id: 11, model: 'qwen-vl-max' }, textProvider: { id: 12, model: 'deepseek-chat' } })
    const createObjectURL=vi.spyOn(URL,'createObjectURL').mockReturnValue('blob:vision-preview')
    const revokeObjectURL=vi.spyOn(URL,'revokeObjectURL').mockImplementation(()=>{})
    const wrapper = mount(ReaderView, { global: { stubs: { RouterLink: true, ReaderPdf: true } } })
    await flushPromises();await wrapper.findAll('button').find(button=>button.text()==='AI')!.trigger('click')
    const fileInput=wrapper.get('input[type="file"]')
    Object.defineProperty(fileInput.element,'value',{value:'C:\\fakepath\\page.png',writable:true,configurable:true})
    const image=new File(['png-bytes'],'page.png',{type:'image/png'})
    await wrapper.get('[data-test="vision-paste-zone"]').trigger('paste',{clipboardData:{files:[image]}})
    expect(createObjectURL).toHaveBeenCalledWith(image);expect(wrapper.get('[data-test="vision-preview"]').attributes('src')).toBe('blob:vision-preview')
    await wrapper.get('[aria-label="视觉问题"]').setValue('解释这张图')
    await wrapper.get('[data-test="run-vision"]').trigger('click');await flushPromises()
    expect(providersApi.runVisionAction).toHaveBeenCalledWith(7,expect.any(FormData))
    expect((fileInput.element as HTMLInputElement).value).toBe('')
    const form=providersApi.runVisionAction.mock.calls[0][1] as FormData
    expect(form.get('action')).toBe('DIRECT');expect(form.get('question')).toBe('解释这张图');expect(form.get('screenshot')).toBe(image)
    expect(wrapper.find('[data-test="vision-preview"]').exists()).toBe(false);expect(revokeObjectURL).toHaveBeenCalledWith('blob:vision-preview')
    expect(wrapper.text()).toContain('视觉识别已完成')
  })

  it('clears transient screenshot and revokes every preview URL on failure, route navigation, and unmount', async () => {
    providersApi.getVisualProviderState.mockResolvedValue({ visionProvider: { id: 11, model: 'qwen-vl-max' }, textProvider: { id: 12, model: 'deepseek-chat' } })
    providersApi.runVisionAction.mockRejectedValueOnce(new Error('safe failure'))
    vi.spyOn(URL,'createObjectURL').mockReturnValueOnce('blob:failure').mockReturnValueOnce('blob:navigation').mockReturnValueOnce('blob:unmount')
    const revoke=vi.spyOn(URL,'revokeObjectURL').mockImplementation(()=>{})
    const mountReader=async()=>{const wrapper=mount(ReaderView,{global:{stubs:{RouterLink:true,ReaderPdf:true}}});await flushPromises();await wrapper.findAll('button').find(button=>button.text()==='AI')!.trigger('click');return wrapper}

    const failed=await mountReader();await failed.get('[data-test="vision-paste-zone"]').trigger('paste',{clipboardData:{files:[new File(['a'],'a.png',{type:'image/png'})]}})
    await failed.get('[data-test="run-vision"]').trigger('click');await flushPromises()
    expect(failed.find('[data-test="vision-preview"]').exists()).toBe(false);expect(failed.text()).toContain('视觉操作失败，请检查模型设置或稍后重试');failed.unmount()

    const navigating=await mountReader();await navigating.get('[data-test="vision-paste-zone"]').trigger('paste',{clipboardData:{files:[new File(['b'],'b.png',{type:'image/png'})]}})
    routeLeave.callback?.();await navigating.vm.$nextTick();expect(navigating.find('[data-test="vision-preview"]').exists()).toBe(false);navigating.unmount()

    const unmounting=await mountReader();await unmounting.get('[data-test="vision-paste-zone"]').trigger('paste',{clipboardData:{files:[new File(['c'],'c.png',{type:'image/png'})]}})
    unmounting.unmount()
    expect(revoke).toHaveBeenCalledWith('blob:failure');expect(revoke).toHaveBeenCalledWith('blob:navigation');expect(revoke).toHaveBeenCalledWith('blob:unmount')
  })
})
