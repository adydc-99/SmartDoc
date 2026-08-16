import {describe,expect,it,vi} from 'vitest'
import type {PDFDocumentProxy} from 'pdfjs-dist'
import {extractPdfText,renderPdfCanvas} from '../pdf'

describe('PDF rendering phases',()=>{
  it('renders the canvas without waiting for or requesting text content',async()=>{
    let finishCanvas:()=>void=()=>{}
    const renderPromise=new Promise<void>(resolve=>{finishCanvas=resolve})
    const page={
      getViewport:vi.fn(()=>({width:600,height:800})),
      getTextContent:vi.fn(),
      render:vi.fn(()=>({promise:renderPromise})),
    }
    const document={getPage:vi.fn().mockResolvedValue(page)} as unknown as PDFDocumentProxy
    const canvas={width:0,height:0,getContext:vi.fn(()=>({}))} as unknown as HTMLCanvasElement

    const pending=renderPdfCanvas(document,1,canvas,1)
    await Promise.resolve()

    expect(page.getTextContent).not.toHaveBeenCalled()
    finishCanvas()
    await expect(pending).resolves.toEqual({width:600,height:800})
    expect(canvas.width).toBe(600)
    expect(canvas.height).toBe(800)
  })

  it('extracts page text without starting a canvas render',async()=>{
    const page={
      getTextContent:vi.fn().mockResolvedValue({items:[{str:'Hello'},{str:'PDF'},{}]}),
      render:vi.fn(),
    }
    const document={getPage:vi.fn().mockResolvedValue(page)} as unknown as PDFDocumentProxy

    await expect(extractPdfText(document,1)).resolves.toBe('Hello PDF')
    expect(page.render).not.toHaveBeenCalled()
  })
})
