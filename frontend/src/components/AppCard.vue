<!--
  AppCard — white surface card built from design tokens (radius / shadow / padding).
  Lightweight alternative to ion-card for the card-list screens (balances, groups,
  expenses). Slot-only; no logic.

  Props:
    - padding?: token-based padding override (default --space-5).
-->
<template>
  <div
    class="bg-surface border border-border rounded-lg shadow-sm"
    :style="cardStyle"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  padding?: string;
}>();

// Padding kept as a style so the prop override stays dynamic; default --space-5.
const cardStyle = computed(() => ({
  padding: props.padding ?? 'var(--space-5)',
}));
</script>
