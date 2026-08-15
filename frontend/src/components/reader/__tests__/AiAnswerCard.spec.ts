import {mount} from '@vue/test-utils'
import {describe,expect,it} from 'vitest'
import AiAnswerCard from '../AiAnswerCard.vue'
import type {AiResult,AiSource} from '../../../api/study'

const sources:AiSource[]=Array.from({length:5},(_,index)=>({
  documentId:7,pageNumber:index+1,chunkIndex:index,text:`第 ${index+1} 条原文证据`,relevance:index===0?'HIGH':'RELATED',
}))
const result:AiResult={id:1,action:'ASK',content:'**缓存答案**',mode:'DEMO',cached:false,createdAt:'2026-08-15T10:00:00',source:sources[0],sources}

describe('AiAnswerCard',()=>{
  it('shows three citations initially and expands and collapses the remaining sources',async()=>{
    const wrapper=mount(AiAnswerCard,{props:{result,busy:false}})
    expect(wrapper.findAll('[data-test="citation-card"]')).toHaveLength(3)
    expect(wrapper.get('[data-test="toggle-citations"]').text()).toContain('另外 2 条')
    await wrapper.get('[data-test="toggle-citations"]').trigger('click')
    expect(wrapper.findAll('[data-test="citation-card"]')).toHaveLength(5)
    expect(wrapper.get('[data-test="toggle-citations"]').text()).toContain('收起')
    await wrapper.get('[data-test="toggle-citations"]').trigger('click')
    expect(wrapper.findAll('[data-test="citation-card"]')).toHaveLength(3)
  })

  it('renders sanitized markdown and emits semantic citation navigation',async()=>{
    const unsafe={...result,content:'**答案**<script>alert(1)</script>'}
    const wrapper=mount(AiAnswerCard,{props:{result:unsafe,busy:false}})
    expect(wrapper.get('[data-test="answer-content"]').html()).toContain('<strong>答案</strong>')
    expect(wrapper.html()).not.toContain('<script>')
    expect(wrapper.get('[data-test="citation-card"]').element.tagName).toBe('BUTTON')
    await wrapper.get('[data-test="citation-card"]').trigger('click')
    expect(wrapper.emitted('navigate')?.[0]).toEqual([sources[0]])
    await wrapper.get('[data-test="regenerate-answer"]').trigger('click')
    expect(wrapper.emitted('regenerate')).toHaveLength(1)
  })

  it('falls back to the legacy single source response',()=>{
    const legacy={...result,sources:undefined}
    const wrapper=mount(AiAnswerCard,{props:{result:legacy,busy:false}})
    expect(wrapper.findAll('[data-test="citation-card"]')).toHaveLength(1)
    expect(wrapper.get('[data-test="citation-card"]').text()).toContain('第 1 页')
    expect(wrapper.find('[data-test="toggle-citations"]').exists()).toBe(false)
  })
})
