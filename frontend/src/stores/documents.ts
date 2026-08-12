import { defineStore } from 'pinia'
import type { LocationQuery, LocationQueryRaw } from 'vue-router'
import * as libraryApi from '../api/library'
import type { DeleteImpact, LibraryDocument, LibraryFilters } from '../api/library'

export interface DocumentFilters extends LibraryFilters {
  sort: 'updated-desc' | 'updated-asc' | 'name-asc' | 'name-desc'
}

export type UploadState = 'validating' | 'uploading' | 'parsing' | 'success' | 'failure'
export interface UploadItem { id: string; file: File; state: UploadState; progress: number; error?: string }

const first = (value: LocationQuery[string]) => Array.isArray(value) ? value[0] : value
const numberValue = (value: LocationQuery[string]) => {
  const parsed = Number(first(value))
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

export function filtersFromQuery(query: LocationQuery): DocumentFilters {
  const sort = first(query.sort)
  return {
    query: first(query.query) || undefined,
    type: first(query.type) || undefined,
    favorite: first(query.favorite) === 'true' ? true : undefined,
    folderId: numberValue(query.folderId),
    tagId: numberValue(query.tagId),
    sort: sort === 'updated-asc' || sort === 'name-asc' || sort === 'name-desc' ? sort : 'updated-desc',
  }
}

export function filtersToQuery(filters: DocumentFilters): LocationQueryRaw {
  return Object.fromEntries(Object.entries(filters)
    .filter(([, value]) => value !== undefined && value !== '' && !(value === 'updated-desc'))
    .map(([key, value]) => [key, String(value)]))
}

export async function runUploadQueue(files: File[], upload: (file: File) => Promise<void>) {
  let cursor = 0
  const worker = async () => {
    while (cursor < files.length) {
      const file = files[cursor]
      cursor += 1
      await upload(file)
    }
  }
  await Promise.all([worker(), worker()])
}

export const useDocumentsStore = defineStore('documents', {
  state: () => ({
    folders: [] as libraryApi.FolderNode[], tags: [] as libraryApi.Tag[], documents: [] as LibraryDocument[],
    filters: { sort: 'updated-desc' } as DocumentFilters,
    uploads: [] as UploadItem[], loading: false, error: '',
  }),
  actions: {
    async loadFolders() { this.folders = await libraryApi.listFolders() },
    async loadTags() { this.tags = await libraryApi.listTags() },
    async loadDocuments() {
      this.loading = true; this.error = ''
      try { this.documents = await libraryApi.listLibraryDocuments(this.filters) }
      catch (error) { this.error = error instanceof Error ? error.message : '资料加载失败' }
      finally { this.loading = false }
    },
    async uploadFiles(files: File[]) {
      const allowed = /\.(pdf|md|markdown|txt|java|xml|ya?ml|sql|js|ts|json|properties|sh|ps1)$/i
      const items = files.map((file, index): UploadItem => ({ id: `${Date.now()}-${index}`, file, state: 'validating', progress: 0 }))
      this.uploads.push(...items)
      await runUploadQueue(items.map((item) => item.file), async (file) => {
        const item = items.find((candidate) => candidate.file === file)
        if (!item) return
        if (!allowed.test(file.name)) { item.state = 'failure'; item.error = '不支持该文件类型'; return }
        item.state = 'uploading'
        try {
          await libraryApi.uploadDocument(file, this.filters.folderId, (progress) => { item.progress = progress })
          item.state = 'parsing'; item.progress = 100
        } catch (error) { item.state = 'failure'; item.error = error instanceof Error ? error.message : '上传失败' }
      })
      await this.loadDocuments()
      for (const item of items) {
        if (item.state !== 'parsing') continue
        const document = this.documents.find((candidate) => candidate.name === item.file.name)
        if (document?.status === 'READY') item.state = 'success'
        if (document?.status === 'FAILED') { item.state = 'failure'; item.error = '资料解析失败' }
      }
    },
    async retryUpload(id: string) {
      const item = this.uploads.find((candidate) => candidate.id === id)
      if (!item) return
      item.state = 'validating'; item.error = undefined; item.progress = 0
      await this.uploadFiles([item.file])
    },
    async organizeDocument(id: number, update: libraryApi.OrganizationUpdate) { await libraryApi.organizeDocument(id, update); await this.loadDocuments() },
    async previewDelete(id: number): Promise<DeleteImpact> { return libraryApi.getDeleteImpact(id) },
    async deleteDocument(id: number) { await libraryApi.removeDocument(id); await this.loadDocuments() },
  },
})
