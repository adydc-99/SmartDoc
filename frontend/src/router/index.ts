import {createRouter,createWebHistory,type RouteRecordRaw} from 'vue-router'
import WorkspaceLayout from '../layouts/WorkspaceLayout.vue'
const routes:RouteRecordRaw[]=[{path:'/',component:WorkspaceLayout,children:[
 {path:'',component:()=>import('../views/DashboardView.vue'),meta:{title:'工作台'}},
 {path:'library',component:()=>import('../views/LibraryView.vue'),meta:{title:'资料库'}},
 {path:'reader/:id',component:()=>import('../views/ReaderView.vue'),meta:{title:'阅读器'}},
 {path:'recent',component:()=>import('../views/LibraryView.vue'),meta:{title:'最近阅读'}},
 {path:'favorites',component:()=>import('../views/LibraryView.vue'),meta:{title:'收藏'}},
 {path:'notes',component:()=>import('../views/NotesView.vue'),meta:{title:'全部笔记'}},
 {path:'review',component:()=>import('../views/ReviewView.vue'),meta:{title:'复习'}},
 {path:'ai',component:()=>import('../views/AiChatView.vue'),meta:{title:'AI 对话'}},
 {path:'settings',component:()=>import('../views/SettingsView.vue'),meta:{title:'设置'}},
]},{path:'/:pathMatch(.*)*',redirect:'/'}]
export {routes};const router=createRouter({history:createWebHistory(),routes});router.afterEach(to=>{document.title=`${String(to.meta.title??'SmartDoc')} · SmartDoc`});export default router
