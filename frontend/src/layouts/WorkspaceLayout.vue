<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { login, messageOf } from '../api'
import AppIcon from '../components/AppIcon.vue'
import AppSidebar from '../components/AppSidebar.vue'
const token = ref(localStorage.getItem('smartdoc-token') ?? '')
const username = ref('demo'); const password = ref('smartdoc123'); const error = ref(''); const pending = ref(false)
const drawerOpen = ref(false); const dark = ref(localStorage.getItem('smartdoc-theme') === 'dark')
const applyTheme = () => document.documentElement.classList.toggle('dark', dark.value)
const toggleTheme = () => { dark.value = !dark.value; localStorage.setItem('smartdoc-theme', dark.value ? 'dark' : 'light'); applyTheme() }
const submit = async () => { pending.value = true; error.value = ''; try { const result = await login(username.value, password.value); localStorage.setItem('smartdoc-token', result.token); token.value = result.token } catch (reason) { error.value = messageOf(reason) } finally { pending.value = false } }
onMounted(() => { applyTheme(); window.addEventListener('smartdoc-auth-expired', () => { token.value = '' }) })
</script>
<template>
  <main v-if="!token" class="login-page">
    <section class="login-intro"><div class="brand brand-light"><span class="brand-symbol">S</span>SmartDoc</div><p class="eyebrow">个人知识工作台</p><h1>阅读、整理，<br><span>让知识留下来。</span></h1><p>把 PDF、Markdown、文本与代码资料集中到一个安静、可追溯的学习空间。</p></section>
    <section class="login-panel"><form class="login-card" @submit.prevent="submit"><p class="eyebrow">欢迎回来</p><h2>进入工作台</h2><p class="muted">使用本地 SmartDoc 账户登录。</p><label>用户名<input v-model="username" autocomplete="username" /></label><label>密码<input v-model="password" type="password" autocomplete="current-password" /></label><p v-if="error" class="form-error" role="alert">{{ error }}</p><button class="button primary" :disabled="pending">{{ pending ? '正在登录…' : '登录' }}</button><small>演示账户：demo / smartdoc123</small></form></section>
  </main>
  <div v-else class="workspace-shell">
    <a class="skip-link" href="#main-content">跳到主要内容</a>
    <AppSidebar :open="drawerOpen" @toggle="drawerOpen = !drawerOpen" @close="drawerOpen = false" />
    <div class="workspace-frame"><header class="topbar"><div><p class="eyebrow">SMARTDOC</p><strong>学习资料工作台</strong></div><div class="topbar-actions"><span class="status-pill">本地模式</span><button class="icon-button" :aria-label="dark ? '切换浅色模式' : '切换深色模式'" :title="dark ? '浅色模式' : '深色模式'" @click="toggleTheme"><AppIcon :name="dark ? 'sun' : 'moon'" /></button></div></header><main id="main-content" tabindex="-1"><RouterView /></main></div>
  </div>
</template>
