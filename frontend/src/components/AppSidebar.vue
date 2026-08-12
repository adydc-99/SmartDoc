<script setup lang="ts">
import AppIcon from './AppIcon.vue'
defineProps<{ open?: boolean }>()
defineEmits<{ toggle: []; close: [] }>()
const items = [
  ['/', 'home', '工作台'], ['/library', 'library', '资料库'], ['/recent', 'recent', '最近阅读'],
  ['/favorites', 'star', '收藏'], ['/notes', 'note', '全部笔记'], ['/review', 'review', '复习'],
  ['/ai', 'chat', 'AI 对话'], ['/settings', 'settings', '设置'],
]
</script>
<template>
  <button class="mobile-menu icon-button" type="button" aria-label="打开资料导航" :aria-expanded="open ? 'true' : 'false'" @click="$emit('toggle')"><AppIcon name="menu" /></button>
  <div v-if="open" class="drawer-backdrop" @click="$emit('close')" />
  <aside :class="['app-sidebar', { open }]" aria-label="资料导航侧栏">
    <div class="brand"><span class="brand-symbol">S</span><span>SmartDoc<small>学习工作台</small></span><button class="drawer-close icon-button" aria-label="关闭资料导航" @click="$emit('close')"><AppIcon name="close" /></button></div>
    <nav aria-label="主导航">
      <RouterLink v-for="item in items" :key="item[0]" :to="item[0]" @click="$emit('close')"><AppIcon :name="item[1]" /><span>{{ item[2] }}</span></RouterLink>
    </nav>
    <div class="phase-badge"><span>AI</span><div><strong>按需调用</strong><small>不会在后台自动消耗额度</small></div></div>
  </aside>
</template>
