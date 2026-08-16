<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { PDFDocumentProxy } from 'pdfjs-dist'
import { extractPdfText, loadPdf, renderPdfCanvas } from '../../reader/pdf'

const props = defineProps<{ blob: Blob; page: number; zoom: number }>()
const emit = defineEmits<{ pages: [number]; page: [number]; text: [string] }>()
const canvas = ref<HTMLCanvasElement>()
const pdf = ref<PDFDocumentProxy>()
const url = ref('')
const pages = ref(0)
const thumbnailNav = ref<HTMLElement>()
const busy = ref(true)
const renderError = ref('')
const selectableText = ref('')
let renderRequest = 0
let disposed = false
let thumbnailsStarted = false
let canvasRenderQueue = Promise.resolve()

const draw = async () => {
  const document = pdf.value
  const target = canvas.value
  if (!document || !target) return
  const request = ++renderRequest
  const pageNumber = props.page
  busy.value = true
  renderError.value = ''
  selectableText.value = ''
  const previousRender = canvasRenderQueue
  let releaseRender: () => void = () => undefined
  canvasRenderQueue = new Promise<void>((resolve) => { releaseRender = resolve })
  await previousRender
  if (disposed || request !== renderRequest || pdf.value !== document) {
    releaseRender()
    return
  }
  try {
    await renderPdfCanvas(document, pageNumber, target, props.zoom)
    if (disposed || request !== renderRequest || pdf.value !== document) return
    busy.value = false
    if (!thumbnailsStarted) {
      thumbnailsStarted = true
      void renderThumbnails(document)
    }
    void extractPdfText(document, pageNumber).then((text) => {
      if (disposed || request !== renderRequest || pdf.value !== document) return
      selectableText.value = text
      emit('text', text)
    }).catch(() => undefined)
  } catch {
    if (disposed || request !== renderRequest || pdf.value !== document) return
    busy.value = false
    renderError.value = `第 ${pageNumber} 页渲染失败，请重试。`
  } finally {
    releaseRender()
  }
}

const renderThumbnails = async (document: PDFDocumentProxy) => {
  const thumbnails = Array.from(thumbnailNav.value?.querySelectorAll('canvas') ?? [])
  for (let index = 0; index < thumbnails.length; index += 1) {
    if (disposed || pdf.value !== document) return
    const thumbnail = thumbnails[index]
    try {
      await renderPdfCanvas(document, index + 1, thumbnail, 0.18)
    } catch {
      // 单个缩略图失败不应阻塞正文阅读或后续缩略图。
    }
  }
}

onMounted(async () => {
  try {
    const loaded = await loadPdf(props.blob)
    if (disposed) {
      await loaded.document.destroy()
      URL.revokeObjectURL(loaded.url)
      return
    }
    pdf.value = loaded.document
    url.value = loaded.url
    pages.value = loaded.document.numPages
    emit('pages', pages.value)
    await nextTick()
    await draw()
  } catch {
    if (!disposed) {
      busy.value = false
      renderError.value = 'PDF 加载失败，请返回资料库后重试。'
    }
  }
})

watch(() => [props.page, props.zoom], () => void draw())

onBeforeUnmount(() => {
  disposed = true
  renderRequest += 1
  void pdf.value?.destroy()
  if (url.value) URL.revokeObjectURL(url.value)
})
</script>

<template>
  <div class="pdf-reader">
    <nav ref="thumbnailNav" class="pdf-thumbnails" aria-label="PDF 缩略图">
      <small v-if="pages > 20">为保证性能仅预览前 20 页；页码框仍可跳转全部页面。</small>
      <button v-for="n in Math.min(pages, 20)" :key="n" :class="{ active: n === page }" :aria-label="`转到第 ${n} 页`" @click="emit('page', n)">
        <canvas />
        <span>{{ n }}</span>
      </button>
    </nav>
    <section class="pdf-page" aria-live="polite">
      <p v-if="renderError" role="alert">{{ renderError }}</p>
      <p v-else-if="busy">正在渲染第 {{ page }} 页…</p>
      <canvas ref="canvas" />
      <div class="pdf-selectable-text" aria-label="本页可选择文本">{{ selectableText }}</div>
    </section>
  </div>
</template>
