import {flushPromises,mount} from '@vue/test-utils'
import {beforeEach,describe,expect,it,vi} from 'vitest'
import ReaderPdf from '../ReaderPdf.vue'

const pdf=vi.hoisted(()=>({loadPdf:vi.fn(),renderPdfCanvas:vi.fn(),extractPdfText:vi.fn()}))
vi.mock('../../../reader/pdf',()=>pdf)

const deferred=<T>()=>{let resolve!:(value:T)=>void;let reject!:(reason?:unknown)=>void;const promise=new Promise<T>((yes,no)=>{resolve=yes;reject=no});return{promise,resolve,reject}}
const documentProxy=(pages=2)=>({numPages:pages,destroy:vi.fn()})

describe('ReaderPdf resources and scheduling',()=>{
  beforeEach(()=>{vi.clearAllMocks();pdf.renderPdfCanvas.mockResolvedValue({width:600,height:800});pdf.extractPdfText.mockResolvedValue('page text')})

  it('renders the active canvas before thumbnails and does not wait for text',async()=>{
    const activeCanvas=deferred<{width:number;height:number}>(),text=deferred<string>(),document=documentProxy(2)
    pdf.loadPdf.mockResolvedValue({document,url:'blob:reader'})
    pdf.renderPdfCanvas.mockImplementation((_document:unknown,_page:number,_canvas:HTMLCanvasElement,scale:number)=>scale===1?activeCanvas.promise:Promise.resolve({width:108,height:144}))
    pdf.extractPdfText.mockReturnValue(text.promise)

    const wrapper=mount(ReaderPdf,{props:{blob:new Blob(['pdf']),page:1,zoom:1}})
    await flushPromises()

    expect(pdf.renderPdfCanvas).toHaveBeenCalledTimes(1)
    expect(pdf.renderPdfCanvas.mock.calls[0][1]).toBe(1)
    expect(pdf.renderPdfCanvas.mock.calls[0][3]).toBe(1)
    expect(wrapper.text()).toContain('正在渲染第 1 页')

    activeCanvas.resolve({width:600,height:800});await flushPromises();await wrapper.vm.$nextTick();await flushPromises()

    expect(wrapper.text()).not.toContain('正在渲染第 1 页')
    expect(pdf.renderPdfCanvas.mock.calls.map(call=>call[3])).toContain(.18)
    expect(wrapper.emitted('text')).toBeUndefined()
    wrapper.unmount()
  })

  it('prevents stale page text from replacing the latest page',async()=>{
    const first=deferred<string>(),second=deferred<string>(),document=documentProxy(2)
    pdf.loadPdf.mockResolvedValue({document,url:'blob:reader'})
    pdf.extractPdfText.mockImplementation((_document:unknown,page:number)=>page===1?first.promise:second.promise)
    const wrapper=mount(ReaderPdf,{props:{blob:new Blob(['pdf']),page:1,zoom:1}})
    await flushPromises();await wrapper.setProps({page:2});await flushPromises()

    second.resolve('page two');await flushPromises()
    first.resolve('page one');await flushPromises()

    const textEvents = wrapper.emitted('text') ?? []
    expect(textEvents[textEvents.length - 1]).toEqual(['page two'])
    expect(wrapper.text()).toContain('page two')
    wrapper.unmount()
  })

  it('serializes active renders when the page changes quickly',async()=>{
    const firstCanvas=deferred<{width:number;height:number}>(),document=documentProxy(2)
    pdf.loadPdf.mockResolvedValue({document,url:'blob:reader'})
    pdf.renderPdfCanvas.mockImplementation((_document:unknown,page:number,_canvas:HTMLCanvasElement,scale:number)=>page===1&&scale===1?firstCanvas.promise:Promise.resolve({width:600,height:800}))
    const wrapper=mount(ReaderPdf,{props:{blob:new Blob(['pdf']),page:1,zoom:1}})
    await flushPromises();await wrapper.setProps({page:2});await flushPromises()

    expect(pdf.renderPdfCanvas.mock.calls.filter(call=>call[3]===1)).toHaveLength(1)
    firstCanvas.resolve({width:600,height:800});await flushPromises()
    expect(pdf.renderPdfCanvas.mock.calls.some(call=>call[1]===2&&call[3]===1)).toBe(true)
    wrapper.unmount()
  })

  it('destroys the PDF and revokes its object URL on unmount',async()=>{
    const document=documentProxy(1);pdf.loadPdf.mockResolvedValue({document,url:'blob:reader'})
    const revoke=vi.spyOn(URL,'revokeObjectURL').mockImplementation(()=>{})
    const wrapper=mount(ReaderPdf,{props:{blob:new Blob(['pdf']),page:1,zoom:1}})
    await flushPromises();wrapper.unmount()
    expect(document.destroy).toHaveBeenCalled();expect(revoke).toHaveBeenCalledWith('blob:reader')
  })
})
