<script setup lang="ts">
import {computed,ref} from 'vue'
import type {AiResult,AiSource} from '../../api/study'
import {renderDocument} from '../../reader/render'

const props=defineProps<{result:AiResult;busy:boolean}>()
const emit=defineEmits<{navigate:[source:AiSource];regenerate:[]}>()
const expanded=ref(false)
const allSources=computed(()=>props.result.sources?.length?props.result.sources:[props.result.source])
const visibleSources=computed(()=>expanded.value?allSources.value:allSources.value.slice(0,3))
const hiddenCount=computed(()=>Math.max(0,allSources.value.length-3))
const answerHtml=computed(()=>renderDocument('markdown',props.result.content))
const sourceLabel=(source:AiSource,index:number)=>source.pageNumber?`第 ${source.pageNumber} 页`:`片段 ${source.chunkIndex==null?index+1:source.chunkIndex+1}`
const sourceKey=(source:AiSource,index:number)=>`${source.documentId}-${source.chunkIndex??'x'}-${source.pageNumber??'x'}-${index}`
</script>

<template>
  <article class="ai-answer-card">
    <header class="ai-answer-heading">
      <div>
        <p class="eyebrow">AI 回答</p>
        <span class="ai-answer-meta">{{ result.cached?'已从缓存读取':'新生成' }} · {{ result.mode }}</span>
      </div>
      <button data-test="regenerate-answer" class="button secondary compact" type="button" :disabled="busy" @click="emit('regenerate')">重新生成</button>
    </header>
    <div data-test="answer-content" class="ai-answer-content prose" v-html="answerHtml" />
    <section v-if="allSources.length" class="citation-section" aria-labelledby="citation-title">
      <div class="citation-heading">
        <h3 id="citation-title">回答依据</h3>
        <span>{{ allSources.length }} 条来源</span>
      </div>
      <div class="citation-list">
        <button v-for="(source,index) in visibleSources" :key="sourceKey(source,index)" data-test="citation-card" class="citation-card" type="button" @click="emit('navigate',source)">
          <span class="citation-rank">{{ index+1 }}</span>
          <span class="citation-body">
            <span class="citation-meta">
              <strong>{{ sourceLabel(source,index) }}</strong>
              <span class="citation-relevance">{{ source.relevance==='HIGH'?'高度相关':'相关' }}</span>
            </span>
            <span class="citation-excerpt">{{ source.text }}</span>
          </span>
          <span aria-hidden="true" class="citation-arrow">→</span>
        </button>
      </div>
      <button v-if="hiddenCount" data-test="toggle-citations" class="citation-toggle" type="button" :aria-expanded="expanded" @click="expanded=!expanded">
        {{ expanded?'收起来源':`另外 ${hiddenCount} 条来源` }}
      </button>
    </section>
  </article>
</template>
