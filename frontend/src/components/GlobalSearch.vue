<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { globalSearch, type SearchHit } from '../api/search'
import AppIcon from './AppIcon.vue'
const props = withDefaults(defineProps<{ results?: SearchHit[] }>(), { results: () => [] })
const query = ref(''); const remote = ref<SearchHit[]>([]); const loading = ref(false); const error = ref('')
let timer: number | undefined
watch(query, (value) => { window.clearTimeout(timer); if (value.trim().length < 2) { remote.value = []; return } timer = window.setTimeout(async () => { loading.value = true; error.value = ''; try { remote.value = await globalSearch(value.trim()) } catch { error.value = '搜索失败，请重试' } finally { loading.value = false } }, 250) })
const hits = computed(() => props.results.length ? props.results : remote.value)
const groups = computed(() => [
  { key: 'DOCUMENT', title: '资料', items: hits.value.filter((hit) => hit.type === 'DOCUMENT' || hit.type === 'CONTENT') },
  { key: 'NOTE', title: '笔记', items: hits.value.filter((hit) => hit.type === 'NOTE') },
  { key: 'TAG', title: '标签', items: hits.value.filter((hit) => hit.type === 'TAG') },
].filter((group) => group.items.length))
const href = (hit: SearchHit) => hit.pageNumber ? `/reader/${hit.documentId}?page=${hit.pageNumber}` : hit.type === 'NOTE' ? `/notes?documentId=${hit.documentId}` : `/reader/${hit.documentId}`
</script>
<template><div class="global-search"><label><span class="sr-only">全局搜索</span><AppIcon name="search" /><input v-model="query" type="search" placeholder="搜索资料、正文、笔记或标签" /></label><div v-if="loading" class="search-status">正在搜索…</div><div v-else-if="error" class="search-status error">{{ error }}</div><div v-if="groups.length" class="search-popover"><section v-for="group in groups" :key="group.key"><h3>{{ group.title }}</h3><a v-for="hit in group.items" :key="`${hit.type}-${hit.documentId}-${hit.noteId}-${hit.pageNumber}`" :href="href(hit)"><strong>{{ hit.title }}</strong><span>{{ hit.snippet }}</span><small v-if="hit.pageNumber">第 {{ hit.pageNumber }} 页</small></a></section></div></div></template>
