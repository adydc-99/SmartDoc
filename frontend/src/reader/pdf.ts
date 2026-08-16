import type {PDFDocumentProxy,PDFPageProxy} from 'pdfjs-dist'
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
export interface LoadedPdf{document:PDFDocumentProxy;url:string}
export async function loadPdf(blob:Blob):Promise<LoadedPdf>{const url=URL.createObjectURL(blob);try{const pdfjs=await import('pdfjs-dist');pdfjs.GlobalWorkerOptions.workerSrc=workerUrl;return{document:await pdfjs.getDocument(url).promise,url}}catch(error){URL.revokeObjectURL(url);throw error}}
export async function renderPdfCanvas(document:PDFDocumentProxy,pageNumber:number,canvas:HTMLCanvasElement,scale=1){const page:PDFPageProxy=await document.getPage(pageNumber),viewport=page.getViewport({scale}),context=canvas.getContext('2d');canvas.width=Math.floor(viewport.width);canvas.height=Math.floor(viewport.height);if(context)await page.render({canvas,canvasContext:context,viewport}).promise;return{width:viewport.width,height:viewport.height}}
export async function extractPdfText(document:PDFDocumentProxy,pageNumber:number){const page:PDFPageProxy=await document.getPage(pageNumber),text=await page.getTextContent();return text.items.map(item=>'str'in item?item.str:'').filter(Boolean).join(' ')}
