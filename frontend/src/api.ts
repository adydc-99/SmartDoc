import axios from 'axios'

export interface DocumentItem {id:number;name:string;sizeBytes:number;pageCount?:number;status:'PROCESSING'|'READY'|'FAILED';summary?:string;keywords?:string;errorMessage?:string;createdAt:string}
export interface Reference {index:number;pageNumber:number;content:string}
export interface Answer {id:number;answer:string;references:Reference[];createdAt:string}
export interface History {id:number;question:string;answer:string;referencesJson:string;createdAt:string}

const api=axios.create({baseURL:'/api'})
api.interceptors.request.use(config=>{const token=localStorage.getItem('smartdoc-token');if(token) config.headers.Authorization=`Bearer ${token}`;return config})
api.interceptors.response.use(r=>r,err=>{if(err.response?.status===401){localStorage.removeItem('smartdoc-token');location.reload()}return Promise.reject(err)})
export const messageOf=(error:unknown)=>axios.isAxiosError(error)?error.response?.data?.message||error.message:'操作失败'
export async function login(username:string,password:string){return (await api.post('/auth/login',{username,password})).data as {token:string;username:string}}
export async function listDocuments(){return (await api.get('/documents')).data as DocumentItem[]}
export async function getDocument(id:number){return (await api.get(`/documents/${id}`)).data as DocumentItem}
export async function uploadDocument(file:File,onProgress:(n:number)=>void){const data=new FormData();data.append('file',file);return (await api.post('/documents',data,{onUploadProgress:e=>onProgress(e.total?Math.round(e.loaded/e.total*100):0)})).data as DocumentItem}
export async function deleteDocument(id:number){await api.delete(`/documents/${id}`)}
export async function askDocument(id:number,question:string){return (await api.post(`/documents/${id}/questions`,{question})).data as Answer}
export async function getHistory(id:number){return (await api.get(`/documents/${id}/questions`)).data as History[]}
