<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>
<template><div class="page boundary-page"><p class="eyebrow">READER</p><h1>资料阅读器</h1><p>正在准备资料 #{{ route.params.id }}。PDF、Markdown、文本与代码阅读器将在下一任务中接入。</p><RouterLink class="button secondary" to="/library">返回资料库</RouterLink></div></template>
