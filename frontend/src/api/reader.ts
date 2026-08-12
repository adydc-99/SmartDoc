import { api, type DocumentItem } from '../api'
export interface ReaderContent { type:string; language:string; content:string }
export interface Progress { pageNumber:number; scrollRatio:number; zoom:number; updatedAt:string|null }
export interface Note { id:number;documentId:number;pageNumber:number|null;sourceText:string|null;contentMarkdown:string;favorite:boolean;tags:{id:number;name:string;color:string}[] }
export const getReaderDocument=(id:number)=>(api.get<DocumentItem>(`/documents/${id}`)).then(r=>r.data)
export const getReaderContent=async(id:number,type:string):Promise<ReaderContent|Blob>=>type==='PDF'?(await api.get(`/documents/${id}/content`,{responseType:'blob'})).data:(await api.get(`/documents/${id}/content`)).data
export const getProgress=(id:number)=>(api.get<Progress>(`/documents/${id}/progress`)).then(r=>r.data)
export const saveProgress=(id:number,value:Omit<Progress,'updatedAt'>)=>(api.put<Progress>(`/documents/${id}/progress`,value)).then(r=>r.data)
export const listDocumentNotes=(id:number)=>(api.get<Note[]>(`/documents/${id}/notes`)).then(r=>r.data)
