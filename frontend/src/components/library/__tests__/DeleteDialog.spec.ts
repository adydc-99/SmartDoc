import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DeleteDialog from '../DeleteDialog.vue'

describe('DeleteDialog', () => {
  it('shows irreversible impact counts before confirmation', () => {
    const wrapper = mount(DeleteDialog, { props: { open: true, documentName: 'JVM.pdf', impact: { notes: 2, excerpts: 3, questions: 4, aiResults: 5, reviewItems: 0 } } })
    expect(wrapper.text()).toContain('2 篇笔记')
    expect(wrapper.text()).toContain('3 条摘录')
    expect(wrapper.text()).toContain('4 条问答')
    expect(wrapper.text()).toContain('5 条 AI 结果')
    expect(wrapper.get('button[aria-label="确认删除 JVM.pdf"]')).toBeTruthy()
  })
})
