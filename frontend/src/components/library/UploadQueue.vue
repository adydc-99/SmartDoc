<script setup lang="ts">
import type { UploadItem } from '../../stores/documents'
defineProps<{ items: UploadItem[] }>()
defineEmits<{ retry: [id: string] }>()
const label = (state: UploadItem['state']) => ({ validating: '校验中', uploading: '上传中', parsing: '解析中', success: '完成', failure: '失败' }[state])
</script>
<template><section v-if="items.length" class="upload-queue" aria-label="上传队列"><header><strong>上传队列</strong><small>同时上传 2 个文件</small></header><div v-for="item in items" :key="item.id" class="upload-item"><span><strong>{{ item.file.name }}</strong><small>{{ label(item.state) }}<template v-if="item.error"> · {{ item.error }}</template></small></span><progress :value="item.progress" max="100">{{ item.progress }}%</progress><button v-if="item.state === 'failure'" @click="$emit('retry', item.id)">重试</button></div></section></template>
