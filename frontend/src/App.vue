<script setup lang="ts">
import {computed,onBeforeUnmount,onMounted,ref} from 'vue'
import {ArrowRight,ChatDotRound,Delete,Document as DocumentIcon,MagicStick,Plus,Search,UploadFilled} from '@element-plus/icons-vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import * as client from './api'
import type {Answer,DocumentItem,History} from './api'

const token=ref(localStorage.getItem('smartdoc-token')||'')
const username=ref('demo'),password=ref('smartdoc123'),logging=ref(false)
const docs=ref<DocumentItem[]>([]),selected=ref<DocumentItem>(),loading=ref(false),uploading=ref(false),progress=ref(0)
const search=ref(''),question=ref(''),asking=ref(false),history=ref<History[]>([]),lastAnswer=ref<Answer>()
let poll:number|undefined
const filtered=computed(()=>docs.value.filter(d=>d.name.toLowerCase().includes(search.value.toLowerCase())))
const keywords=computed(()=>selected.value?.keywords?.split(',').filter(Boolean)||[])
const formatSize=(v:number)=>v<1024*1024?`${(v/1024).toFixed(1)} KB`:`${(v/1024/1024).toFixed(1)} MB`
const statusText=(s:DocumentItem['status'])=>({PROCESSING:'解析中',READY:'已就绪',FAILED:'解析失败'}[s])

async function doLogin(){logging.value=true;try{const data=await client.login(username.value,password.value);localStorage.setItem('smartdoc-token',data.token);token.value=data.token;await refresh()}catch(e){ElMessage.error(client.messageOf(e))}finally{logging.value=false}}
async function refresh(){loading.value=true;try{docs.value=await client.listDocuments();if(selected.value){const match=docs.value.find(d=>d.id===selected.value?.id);selected.value=match}}catch(e){ElMessage.error(client.messageOf(e))}finally{loading.value=false}}
async function choose(doc:DocumentItem){selected.value=doc;lastAnswer.value=undefined;history.value=doc.status==='READY'?await client.getHistory(doc.id):[]}
async function upload(options:{file:File}){uploading.value=true;progress.value=0;try{const doc=await client.uploadDocument(options.file,n=>progress.value=n);ElMessage.success('上传成功，正在解析');await refresh();await choose(docs.value.find(d=>d.id===doc.id)||doc)}catch(e){ElMessage.error(client.messageOf(e))}finally{uploading.value=false}}
async function remove(doc:DocumentItem){await ElMessageBox.confirm(`确定删除「${doc.name}」吗？`,'删除文档',{type:'warning'});try{await client.deleteDocument(doc.id);if(selected.value?.id===doc.id)selected.value=undefined;await refresh();ElMessage.success('已删除')}catch(e){ElMessage.error(client.messageOf(e))}}
async function ask(){if(!selected.value||!question.value.trim())return;asking.value=true;try{lastAnswer.value=await client.askDocument(selected.value.id,question.value.trim());question.value='';history.value=await client.getHistory(selected.value.id)}catch(e){ElMessage.error(client.messageOf(e))}finally{asking.value=false}}
function logout(){localStorage.removeItem('smartdoc-token');token.value='';selected.value=undefined}
onMounted(async()=>{if(token.value)await refresh();poll=window.setInterval(()=>{if(token.value&&docs.value.some(d=>d.status==='PROCESSING'))refresh()},2500)})
onBeforeUnmount(()=>poll&&clearInterval(poll))
</script>

<template>
  <main v-if="!token" class="login-shell">
    <section class="login-copy"><div class="brand-mark"><MagicStick/> SmartDoc</div><h1>让每一份文档<br><span>开口回答问题</span></h1><p>上传 PDF，自动提炼摘要与关键词，并从原文中找到答案依据。</p><div class="feature-row"><span>PDF 解析</span><span>智能摘要</span><span>溯源问答</span></div></section>
    <section class="login-card"><div><p class="eyebrow">WELCOME BACK</p><h2>登录 SmartDoc</h2><p class="muted">演示账号已为你填好，直接进入即可。</p></div><el-form @submit.prevent="doLogin"><el-form-item label="用户名"><el-input v-model="username" size="large"/></el-form-item><el-form-item label="密码"><el-input v-model="password" type="password" show-password size="large" @keyup.enter="doLogin"/></el-form-item><el-button type="primary" size="large" :loading="logging" @click="doLogin">进入工作台 <el-icon><ArrowRight/></el-icon></el-button></el-form><p class="demo-tip">演示账号：demo / smartdoc123</p></section>
  </main>

  <div v-else class="workspace">
    <header><div class="brand-mark dark"><MagicStick/> SmartDoc <span>AI 文档助手</span></div><div class="header-actions"><span class="mode"><i/> Demo AI 模式</span><button class="avatar">D</button><button class="logout" @click="logout">退出</button></div></header>
    <div class="body">
      <aside><div class="aside-title"><div><p class="eyebrow">LIBRARY</p><h2>我的文档</h2></div><el-upload accept="application/pdf,.pdf" :show-file-list="false" :http-request="upload as any"><el-button circle type="primary" :icon="Plus"/></el-upload></div><el-input v-model="search" placeholder="搜索文件名" :prefix-icon="Search" clearable/>
        <div v-if="uploading" class="uploading"><UploadFilled/><div><b>正在上传</b><el-progress :percentage="progress" :stroke-width="5"/></div></div>
        <div class="doc-list" v-loading="loading"><button v-for="doc in filtered" :key="doc.id" :class="['doc-item',{active:selected?.id===doc.id}]" @click="choose(doc)"><span class="file-icon"><DocumentIcon/></span><span class="doc-copy"><b>{{doc.name}}</b><small>{{formatSize(doc.sizeBytes)}} · {{new Date(doc.createdAt).toLocaleDateString()}}</small><em :class="doc.status.toLowerCase()">{{statusText(doc.status)}}</em></span><el-icon class="chevron"><ArrowRight/></el-icon></button><div v-if="!filtered.length&&!loading" class="empty-list"><DocumentIcon/><p>还没有 PDF 文档</p><small>点击右上角 + 开始上传</small></div></div>
      </aside>
      <section v-if="selected" class="content">
        <div class="doc-head"><div><p class="eyebrow">DOCUMENT</p><h1>{{selected.name}}</h1><p>{{formatSize(selected.sizeBytes)}}<span v-if="selected.pageCount"> · {{selected.pageCount}} 页</span> · 上传于 {{new Date(selected.createdAt).toLocaleString()}}</p></div><el-button text type="danger" :icon="Delete" @click="remove(selected)">删除</el-button></div>
        <div v-if="selected.status==='PROCESSING'" class="state-card"><div class="spinner"/><h2>正在读懂这份文档</h2><p>提取文本并生成摘要，通常只需几秒钟。</p></div>
        <div v-else-if="selected.status==='FAILED'" class="state-card error"><h2>解析没有完成</h2><p>{{selected.errorMessage}}</p></div>
        <template v-else><article class="summary-card"><div class="card-title"><span><MagicStick/></span><div><p class="eyebrow">AI SUMMARY</p><h2>内容摘要</h2></div></div><p class="summary">{{selected.summary}}</p><div class="tags"><span v-for="key in keywords" :key="key">{{key}}</span></div></article>
          <article class="chat-card"><div class="card-title"><span><ChatDotRound/></span><div><p class="eyebrow">ASK SMARTDOC</p><h2>向文档提问</h2></div></div><div class="conversation"><div v-for="item in history" :key="item.id" class="turn"><p class="q">{{item.question}}</p><p class="a">{{item.answer}}</p></div><div v-if="lastAnswer&&!history.some(h=>h.id===lastAnswer?.id)" class="turn"><p class="a">{{lastAnswer.answer}}</p></div><div v-if="!history.length" class="chat-empty"><ChatDotRound/><p>试着问一个文档中的问题</p><small>答案会优先引用与问题最相关的原文片段</small></div></div><div v-if="lastAnswer?.references?.length" class="references"><b>参考来源</b><details v-for="ref in lastAnswer.references" :key="ref.index"><summary>第 {{ref.pageNumber}} 页 · 片段 {{ref.index}}</summary><p>{{ref.content}}</p></details></div><div class="ask-box"><textarea v-model="question" placeholder="例如：这篇文档的核心结论是什么？" @keydown.ctrl.enter="ask"/><el-button type="primary" :loading="asking" :disabled="!question.trim()" @click="ask">提问 <el-icon><ArrowRight/></el-icon></el-button><small>Ctrl + Enter 发送</small></div></article></template>
      </section>
      <section v-else class="welcome"><div class="welcome-icon"><MagicStick/></div><p class="eyebrow">SMARTDOC WORKSPACE</p><h1>选择一份文档开始阅读</h1><p>上传 PDF 后，SmartDoc 会自动生成摘要、关键词，<br>并为每个回答提供原文依据。</p><el-upload accept="application/pdf,.pdf" :show-file-list="false" :http-request="upload as any"><el-button type="primary" size="large" :icon="Plus">上传第一份 PDF</el-button></el-upload></section>
    </div>
  </div>
</template>
