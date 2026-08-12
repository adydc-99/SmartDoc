<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDocumentsStore, filtersFromQuery, filtersToQuery, type DocumentFilters } from '../stores/documents'
import type { DeleteImpact, LibraryDocument } from '../api/library'
import FolderTree from '../components/library/FolderTree.vue'
import DocumentToolbar from '../components/library/DocumentToolbar.vue'
import DocumentList from '../components/library/DocumentList.vue'
import UploadQueue from '../components/library/UploadQueue.vue'
import DeleteDialog from '../components/library/DeleteDialog.vue'
const route = useRoute(); const router = useRouter(); const store = useDocumentsStore()
const view = ref<'list'|'card'>('list'); const folderDrawer = ref(false); const selected = ref<number[]>([])
const deleting = ref<LibraryDocument>(); const impact = ref<DeleteImpact | null>(null)
let syncing = false; let filterTimer: number | undefined
const setFilters = (filters: DocumentFilters) => { store.filters = filters }
const chooseFolder = (folderId?: number) => { store.filters = { ...store.filters, folderId }; folderDrawer.value = false }
const toggleSelection = (id: number) => { selected.value = selected.value.includes(id) ? selected.value.filter((item) => item !== id) : [...selected.value, id] }
const notice = ref('')
const showAction = async (kind: 'rename'|'move'|'delete', document: LibraryDocument) => {
  if (kind === 'delete') { deleting.value = document; impact.value = await store.previewDelete(document.id); return }
  if (kind === 'move') { const folder = store.folders[0]; if (folder) { await store.organizeDocument(document.id, { folderId: folder.id }); notice.value = `已移动「${document.name}」到「${folder.name}」` } else notice.value = '请先创建文件夹再移动资料'; return }
  notice.value = '当前服务端尚未提供资料重命名接口；原文件名保持不变。'
}
const confirmDelete = async () => { if (!deleting.value) return; await store.deleteDocument(deleting.value.id); deleting.value = undefined; impact.value = null }
watch([() => route.query, () => route.path], ([query, path]) => { if (syncing) return; store.filters = filtersFromQuery(query); if (path === '/favorites') store.filters.favorite = true; void store.loadDocuments() }, { immediate: true })
watch(() => store.filters, (filters) => { window.clearTimeout(filterTimer); filterTimer = window.setTimeout(async () => { syncing = true; await router.replace({ query: filtersToQuery(filters) }); syncing = false; await store.loadDocuments() }, 250) }, { deep: true })
onMounted(() => { void Promise.all([store.loadFolders(), store.loadTags()]) })
</script>
<template><div class="page library-page"><header class="page-heading"><div><p class="eyebrow">LIBRARY</p><h1>{{ route.path === '/favorites' ? '收藏资料' : route.path === '/recent' ? '最近阅读' : '资料库' }}</h1><p>集中整理课程资料、技术文档与学习摘录。</p></div><span class="metric-inline"><strong>{{ store.documents.length }}</strong> 份资料</span></header><p v-if="notice" class="inline-notice" role="status">{{ notice }} <button aria-label="关闭提示" @click="notice = ''">关闭</button></p><div class="library-grid"><aside class="folder-panel"><h2>文件夹</h2><FolderTree :folders="store.folders" :selected="store.filters.folderId" @select="chooseFolder" /></aside><section class="library-content"><DocumentToolbar :model-value="store.filters" :view="view" :tags="store.tags" @update:model-value="setFilters" @update:view="view = $event" @folders="folderDrawer = true" @upload="store.uploadFiles"/><div v-if="selected.length" class="batch-bar"><strong>已选择 {{ selected.length }} 项</strong><button @click="selected = []">取消选择</button></div><DocumentList :documents="store.documents" :view="view" :loading="store.loading" :error="store.error" :selected="selected" @retry="store.loadDocuments" @select="toggleSelection" @favorite="store.organizeDocument($event.id,{favorite: !$event.favorite})" @action="showAction"/><UploadQueue :items="store.uploads" @retry="store.retryUpload"/></section></div><div v-if="folderDrawer" class="mobile-folder-drawer"><button aria-label="关闭文件夹" @click="folderDrawer = false">关闭</button><FolderTree :folders="store.folders" :selected="store.filters.folderId" @select="chooseFolder" /></div><DeleteDialog :open="Boolean(deleting)" :document-name="deleting?.name ?? ''" :impact="impact" @close="deleting = undefined" @confirm="confirmDelete"/></div></template>
