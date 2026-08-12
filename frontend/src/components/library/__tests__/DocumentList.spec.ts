import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DocumentList from '../DocumentList.vue'
import type { LibraryDocument } from '../../../api/library'

const documents: LibraryDocument[] = [
  { id: 1, name: 'JVM.pdf', sizeBytes: 2048, pageCount: 5, status: 'READY', documentType: 'PDF', mimeType: 'application/pdf', favorite: false, folderId: null, createdAt: '2026-08-12T10:00:00', updatedAt: '2026-08-12T10:00:00', tags: [] },
  { id: 2, name: 'Vue.md', sizeBytes: 1024, pageCount: null, status: 'READY', documentType: 'MARKDOWN', mimeType: 'text/markdown', favorite: true, folderId: null, createdAt: '2026-08-12T10:00:00', updatedAt: '2026-08-12T10:00:00', tags: [] },
]

describe('DocumentList', () => {
  it('keeps selection and document actions visible without hover', () => {
    const wrapper = mount(DocumentList, { props: { documents, view: 'list' } })
    expect(wrapper.findAll('[data-document-row]')).toHaveLength(2)
    expect(wrapper.get('[aria-label="收藏 JVM.pdf"]')).toBeTruthy()
    expect(wrapper.get('[aria-label="打开 JVM.pdf"]')).toBeTruthy()
    expect(wrapper.get('[aria-label="更多操作 JVM.pdf"]')).toBeTruthy()
  })

  it('shows loading, empty, and error retry states', async () => {
    const wrapper = mount(DocumentList, { props: { documents: [], view: 'list', loading: true } })
    expect(wrapper.get('[aria-label="正在加载资料"]')).toBeTruthy()
    await wrapper.setProps({ loading: false, error: '网络不可用' })
    expect(wrapper.text()).toContain('网络不可用')
    expect(wrapper.get('button[aria-label="重试加载资料"]')).toBeTruthy()
    await wrapper.setProps({ error: '' })
    expect(wrapper.text()).toContain('资料库还是空的')
  })
})
