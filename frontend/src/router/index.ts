import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import WorkspaceLayout from '../layouts/WorkspaceLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import LibraryView from '../views/LibraryView.vue'
import ReaderView from '../views/ReaderView.vue'
import NotesView from '../views/NotesView.vue'
import ReviewView from '../views/ReviewView.vue'
import AiChatView from '../views/AiChatView.vue'
import SettingsView from '../views/SettingsView.vue'

export const routes: RouteRecordRaw[] = [{ path: '/', component: WorkspaceLayout, children: [
  { path: '', component: DashboardView, meta: { title: '工作台' } },
  { path: 'library', component: LibraryView, meta: { title: '资料库' } },
  { path: 'reader/:id', component: ReaderView, meta: { title: '阅读器' } },
  { path: 'recent', component: LibraryView, meta: { title: '最近阅读' } },
  { path: 'favorites', component: LibraryView, meta: { title: '收藏' } },
  { path: 'notes', component: NotesView, meta: { title: '全部笔记' } },
  { path: 'review', component: ReviewView, meta: { title: '复习' } },
  { path: 'ai', component: AiChatView, meta: { title: 'AI 对话' } },
  { path: 'settings', component: SettingsView, meta: { title: '设置' } },
] }, { path: '/:pathMatch(.*)*', redirect: '/' }]
const router = createRouter({ history: createWebHistory(), routes })
router.afterEach((to) => { document.title = `${String(to.meta.title ?? 'SmartDoc')} · SmartDoc` })
export default router
