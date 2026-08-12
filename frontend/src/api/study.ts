import {api} from '../api';import type {Note} from './reader'
export type AiAction='ASK'|'SUMMARIZE_DOCUMENT'|'SUMMARIZE_PAGE'|'EXPLAIN_SELECTION'|'SUMMARIZE_SELECTION'|'EXPLAIN_CODE'
export interface AiResult{id:number;action:AiAction;content:string;mode:string;cached:boolean;createdAt:string;source:{documentId:number;pageNumber:number|null;text:string}}
export interface AiSettings{mode:'DEMO'|'DEEPSEEK';baseUrl:string;model:string;maskedKey:string;keyConfigured:boolean;persistenceAvailable:boolean;persistKey:boolean;maxOutputTokens:number;dailyLimit:number;todayUsed:number}
export const searchNotes=(params:Record<string,unknown>)=>(api.get<Note[]>('/notes',{params})).then(r=>r.data)
export const createNote=(id:number,value:unknown)=>(api.post<Note>(`/documents/${id}/notes`,value)).then(r=>r.data)
export const updateNote=(id:number,value:unknown)=>(api.patch<Note>(`/notes/${id}`,value)).then(r=>r.data)
export const deleteNote=(id:number)=>api.delete(`/notes/${id}`)
export const exportNotes=(documentId:number)=>api.get('/notes/export',{params:{documentId},responseType:'blob'})
export const runAi=(documentId:number,value:{action:AiAction;question?:string;selectedText?:string;pageNumber?:number;force:boolean})=>(api.post<AiResult>(`/documents/${documentId}/ai/actions`,value)).then(r=>r.data)
export const getAiSettings=()=>(api.get<AiSettings>('/settings/ai')).then(r=>r.data)
export interface AiSettingsUpdate{mode:'DEMO'|'DEEPSEEK';baseUrl:string;model:string;apiKey:string;persistKey:boolean;maxOutputTokens:number;dailyLimit:number}
export const saveAiSettings=(value:AiSettingsUpdate)=>(api.put<AiSettings>('/settings/ai',value)).then(r=>r.data)
export const clearAiKey=()=>api.delete('/settings/ai/key');export const testAi=()=>api.post<{success:boolean;message:string}>('/settings/ai/test').then(r=>r.data)
