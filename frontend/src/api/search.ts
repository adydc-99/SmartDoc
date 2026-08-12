import { api } from '../api'

export type SearchHitType = 'DOCUMENT' | 'CONTENT' | 'NOTE' | 'TAG'
export interface SearchHit { type: SearchHitType; documentId: number; noteId: number | null; pageNumber: number | null; title: string; snippet: string }
export async function globalSearch(query: string, limit = 30) { return (await api.get<SearchHit[]>('/search', { params: { q: query, limit } })).data }

export interface NoteView { id: number; documentId: number; pageNumber: number | null; sourceText: string | null; contentMarkdown: string; favorite: boolean; createdAt: string; updatedAt: string }
export interface RecentDocument { id: number; name: string; documentType: string; pageCount: number | null; lastOpenedAt: string; updatedAt: string }
export async function recentNotes() { return (await api.get<NoteView[]>('/notes')).data }
export async function recentReading() { return (await api.get<RecentDocument[]>('/reader/recent')).data }
