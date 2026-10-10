<script setup>
import { tournaments } from '@/mock/tournaments'
</script>

<template>
  <main class="page">
    <h1>Tournaments</h1>
    <p>หน้าตัวอย่าง — จะถูกแทนที่ด้วยหน้าจาก Figma</p>
    <ul>
      <li v-for="t in tournaments" :key="t.id">
        {{ t.name }} · {{ t.game }} · {{ t.status }}
      </li>
    </ul>
  </main>
</template>

<style scoped>
.page { max-width: 960px; margin: 0 auto; padding: 32px 16px; }
</style>
