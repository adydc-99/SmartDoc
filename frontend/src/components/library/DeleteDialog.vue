<script setup lang="ts">
import type { DeleteImpact } from '../../api/library'
defineProps<{ open: boolean; documentName: string; impact: DeleteImpact | null }>()
defineEmits<{ close: []; confirm: [] }>()
</script>
<template><div v-if="open" class="modal-overlay" role="presentation"><section class="modal-card" role="dialog" aria-modal="true" aria-labelledby="delete-title"><p class="eyebrow danger-text">不可撤销</p><h2 id="delete-title">删除「{{ documentName }}」？</h2><p>资料文件及下列关联内容都会永久删除：</p><ul v-if="impact"><li>{{ impact.notes }} 篇笔记</li><li>{{ impact.excerpts }} 条摘录</li><li>{{ impact.questions }} 条问答</li><li>{{ impact.aiResults }} 条 AI 结果</li><li>{{ impact.reviewItems }} 条复习项</li></ul><div class="modal-actions"><button class="button secondary" @click="$emit('close')">取消</button><button class="button danger" :aria-label="`确认删除 ${documentName}`" @click="$emit('confirm')">确认删除</button></div></section></div></template>
