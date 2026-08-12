import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ReaderView from '../ReaderView.vue'

const api = vi.hoisted(() => ({
  getReaderDocument: vi.fn(),
  getReaderContent: vi.fn(),
  getProgress: vi.fn(),
  listDocumentNotes: vi.fn(),
  saveProgress: vi.fn(),
}))

vi.mock('../../api/reader', () => api)
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '7' }, query: {} }),
  onBeforeRouteLeave: vi.fn(),
}))

describe('ReaderView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.getReaderDocument.mockResolvedValue({ id: 7, name: 'guide.txt', documentType: 'TXT', pageCount: null, status: 'READY' })
    api.getReaderContent.mockResolvedValue({ type: 'text', language: 'text', content: 'Readable content' })
    api.getProgress.mockResolvedValue({ pageNumber: 1, scrollRatio: 0.42, zoom: 1.25, updatedAt: '2026-08-12T12:00:00' })
    api.listDocumentNotes.mockResolvedValue([])
  })

  it('restores reading progress and retains the mobile note drawer action', async () => {
    const wrapper = mount(ReaderView, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })
    await flushPromises()

    expect(api.getReaderDocument).toHaveBeenCalledWith(7)
    expect(api.getReaderContent).toHaveBeenCalledWith(7, 'TXT')
    expect(wrapper.get('[aria-label="缩放比例"]').element).toHaveValue(125)
    expect(wrapper.get('[data-test="reader-scroll"]').attributes('data-restored-ratio')).toBe('0.42')
    expect(wrapper.get('[data-test="mobile-notes-button"]').text()).toContain('笔记')
  })
})
