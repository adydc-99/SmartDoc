import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ReaderView from '../ReaderView.vue'

const readerApi = vi.hoisted(() => ({
  getReaderDocument: vi.fn(), getReaderContent: vi.fn(), getProgress: vi.fn(),
  listDocumentNotes: vi.fn(), saveProgress: vi.fn(), searchDocument: vi.fn(),
}))
const studyApi = vi.hoisted(() => ({
  createNote: vi.fn(), updateNote: vi.fn(), deleteNote: vi.fn(), runAi: vi.fn(),
}))
const routeLeave = vi.hoisted(() => ({ callback: undefined as undefined | (() => void) }))

vi.mock('../../api/reader', () => readerApi)
vi.mock('../../api/study', () => studyApi)
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
    studyApi.runAi.mockResolvedValue({ id: 1, action: 'SUMMARIZE_PAGE', content: 'summary', mode: 'DEMO', cached: true, createdAt: 'now', source: { documentId: 7, pageNumber: 2, text: 'source' } })
    const wrapper = mount(ReaderView, { global: { stubs: { RouterLink: true, ReaderPdf: { template: '<div class="pdf-stub" />', props: ['blob','page','zoom'] } } } })
    await flushPromises(); const next = wrapper.findAll('button').find(button => button.text() === '下一页')!
    await next.trigger('click'); expect(readerApi.saveProgress).toHaveBeenCalledWith(7, expect.objectContaining({ pageNumber: 2 }))
    await wrapper.findAll('button').find(button => button.text() === 'AI')!.trigger('click')
    await wrapper.get('.reader-ai select').setValue('SUMMARIZE_PAGE'); await wrapper.get('.reader-ai .button.primary').trigger('click'); await flushPromises()
    expect(studyApi.runAi).toHaveBeenLastCalledWith(7, expect.objectContaining({ action: 'SUMMARIZE_PAGE', force: false }))
    await wrapper.findAll('.ai-answer button').find(button => button.text() === '强制重新生成')!.trigger('click'); await flushPromises()
    expect(studyApi.runAi).toHaveBeenLastCalledWith(7, expect.objectContaining({ force: true }))
  })
})
