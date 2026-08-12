import {describe,expect,it} from 'vitest'
import {AI_ACTION_OPTIONS,BACKEND_AI_ACTIONS} from '../study'

describe('AI action backend contract',()=>{
  it('uses only exact backend AiAction enum values for every visible action',()=>{
    expect(BACKEND_AI_ACTIONS).toEqual(['ASK','DOCUMENT_SUMMARY','CURRENT_PAGE_SUMMARY','EXPLAIN','SUMMARIZE','EXPLAIN_CODE','LINE_BY_LINE','COMPLEXITY','FIND_ISSUES','GENERATE_EXAMPLE','INTERVIEW_QUESTION'])
    expect(AI_ACTION_OPTIONS.map(option=>option.value)).toEqual(['ASK','DOCUMENT_SUMMARY','CURRENT_PAGE_SUMMARY','EXPLAIN','SUMMARIZE','EXPLAIN_CODE'])
    expect(AI_ACTION_OPTIONS.every(option=>BACKEND_AI_ACTIONS.includes(option.value))).toBe(true)
  })
})
