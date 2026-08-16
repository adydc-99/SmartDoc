import { api } from '../api'

export interface Tag { id: number; name: string; color: string }
export interface FolderNode { id: number; parentId: number | null; name: string; sortOrder: number; children: FolderNode[] }
export interface LibraryDocument {
  id: number; name: string; sizeBytes: number; pageCount: number | null
  status: 'PROCESSING' | 'READY' | 'FAILED' | 'DELETING'
  documentType: string; mimeType: string | null; favorite: boolean; folderId: number | null
  createdAt: string; updatedAt: string; tags: Tag[]
}
export interface LibraryFilters { folderId?: number; tagId?: number; favorite?: boolean; type?: string; sort?: string; query?: string }
export interface OrganizationUpdate { folderId?: number | null; favorite?: boolean; tagIds?: number[] }
export interface DeleteImpact { notes: number; excerpts: number; questions: number; aiResults: number; reviewItems: number }

export async function listFolders() { return (await api.get<FolderNode[]>('/folders')).data }
export async function listTags() { return (await api.get<Tag[]>('/tags')).data }
export const normalizeLibrarySort=(sort?:string)=>sort?.replace(/-([^-]+)$/g,',$1')
export async function listLibraryDocuments(filters: LibraryFilters) { return (await api.get<LibraryDocument[]>('/library/documents', { params: {...filters,sort:normalizeLibrarySort(filters.sort)} })).data }
export async function organizeDocument(id: number, update: OrganizationUpdate) { return (await api.patch<LibraryDocument>(`/documents/${id}/organization`, update)).data }
export async function getDeleteImpact(id: number) { return (await api.get<DeleteImpact>(`/documents/${id}/delete-impact`)).data }
export async function removeDocument(id: number) { await api.delete(`/documents/${id}`) }
export async function uploadDocument(file: File, folderId?: number, onProgress?: (value: number) => void) {
  const body = new FormData(); body.append('file', file)
  return (await api.post<LibraryDocument>('/documents', body, {
    params: folderId ? { folderId } : undefined,
    onUploadProgress: (event) => onProgress?.(event.total ? Math.round(event.loaded / event.total * 100) : 0),
  })).data
}
