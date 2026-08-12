import axios from 'axios'

export interface DocumentItem {
  id: number
  name: string
  sizeBytes: number
  pageCount: number | null
  status: 'PROCESSING' | 'READY' | 'FAILED' | 'DELETING'
  summary?: string | null
  keywords?: string | null
  errorMessage?: string | null
  documentType?: string
  mimeType?: string | null
  favorite?: boolean
  folderId?: number | null
  createdAt: string
  updatedAt?: string
}

export interface Reference { index: number; pageNumber: number; content: string }
export interface Answer { id: number; answer: string; references: Reference[]; createdAt: string }
export interface History { id: number; question: string; answer: string; referencesJson: string; createdAt: string }

export const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('smartdoc-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use((response) => response, (error: unknown) => {
  if (axios.isAxiosError(error) && error.response?.status === 401) {
    localStorage.removeItem('smartdoc-token')
    window.dispatchEvent(new Event('smartdoc-auth-expired'))
  }
  return Promise.reject(error)
})

export const messageOf = (error: unknown) => axios.isAxiosError(error)
  ? String(error.response?.data?.message ?? error.message)
  : '操作失败，请稍后重试'

export async function login(username: string, password: string) {
  return (await api.post<{ token: string; username: string }>('/auth/login', { username, password })).data
}

export async function getDocument(id: number) { return (await api.get<DocumentItem>(`/documents/${id}`)).data }
export async function askDocument(id: number, question: string) { return (await api.post<Answer>(`/documents/${id}/questions`, { question })).data }
export async function getHistory(id: number) { return (await api.get<History[]>(`/documents/${id}/questions`)).data }
