<script setup lang="ts">
import type { FolderNode } from '../../api/library'
defineProps<{ folders: FolderNode[]; selected?: number }>()
defineEmits<{ select: [id?: number] }>()
</script>
<template><nav class="folder-tree" aria-label="资料文件夹"><button :class="{ active: !selected }" @click="$emit('select', undefined)">全部资料</button><template v-for="folder in folders" :key="folder.id"><button :class="{ active: selected === folder.id }" @click="$emit('select', folder.id)">{{ folder.name }}</button><button v-for="child in folder.children" :key="child.id" class="folder-child" :class="{ active: selected === child.id }" @click="$emit('select', child.id)">{{ child.name }}</button></template></nav></template>
