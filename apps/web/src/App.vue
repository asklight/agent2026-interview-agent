<template>
  <RouterView v-slot="{ Component, route }">
    <component :is="Component" :key="route.meta.remountOnPathChange ? route.path : route.matched[0]?.path" />
  </RouterView>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>
