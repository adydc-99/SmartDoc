<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ name: string }>()
const paths: Record<string, string> = {
  home: 'M3 11.5 12 4l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z',
  library: 'M4 5a2 2 0 0 1 2-2h4l2 2h6a2 2 0 0 1 2 2v12H4z',
  recent: 'M12 8v5l3 2M4.9 5a9 9 0 1 1-1.7 9M3 5v5h5',
  star: 'm12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2-4.5-4.4 6.2-.9z',
  note: 'M5 3h14v18H5zM8 8h8M8 12h8M8 16h5',
  review: 'M7 3h10v3h3v15H4V6h3zM8 12h8M8 16h5',
  chat: 'M4 4h16v13H8l-4 4zM8 9h8M8 13h5',
  settings: 'M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6zm8 3 2 1-2 3 .5 2.5-3 1.7-2-1.5-3 .8-1 2.2H7l-1-2.2-3-.8-2 1.5-3-1.7.5-2.5-2-3 2-1-.5-2.5 3-1.7 2 1.5 3-.8L11 2h2l1 2.2 3 .8 2-1.5 3 1.7-.5 2.5z',
  menu: 'M4 7h16M4 12h16M4 17h16',
  search: 'm20 20-4.5-4.5M10.5 18a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15z',
  upload: 'M12 16V4m-4 4 4-4 4 4M5 14v6h14v-6',
  close: 'M5 5l14 14M19 5 5 19',
  grid: 'M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z',
  list: 'M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01',
  more: 'M5 12h.01M12 12h.01M19 12h.01',
  sun: 'M12 4V2m0 20v-2m8-8h2M2 12h2m14-6 1.5-1.5M4.5 19.5 6 18m12 0 1.5 1.5M4.5 4.5 6 6m6 2a4 4 0 1 0 0 8 4 4 0 0 0 0-8z',
  moon: 'M20 15.5A8 8 0 0 1 8.5 4 8 8 0 1 0 11 20a8 8 0 0 0 9-4.5z',
}
const path = computed(() => paths[props.name] ?? paths.note)
</script>
<template><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path :d="path" /></svg></template>
