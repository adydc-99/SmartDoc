import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import GlobalSearch from '../GlobalSearch.vue'

describe('GlobalSearch', () => {
  it('groups document, note, and tag hits and preserves page jumps', () => {
    const wrapper = mount(GlobalSearch, { props: { results: [
      { type: 'DOCUMENT', documentId: 1, noteId: null, pageNumber: 3, title: 'JVM', snippet: 'memory model' },
      { type: 'NOTE', documentId: 1, noteId: 4, pageNumber: 8, title: 'GC note', snippet: 'collector' },
      { type: 'TAG', documentId: 2, noteId: null, pageNumber: null, title: 'Java', snippet: 'tag' },
    ] } })
    expect(wrapper.text()).toContain('资料')
    expect(wrapper.text()).toContain('笔记')
    expect(wrapper.text()).toContain('标签')
    expect(wrapper.get('a[href="/reader/1?page=3"]')).toBeTruthy()
  })
})
