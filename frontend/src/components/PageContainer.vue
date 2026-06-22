<!--
  PageContainer — reusable page shell for Split-it.

  Renders ion-page + (optional) ion-header/ion-toolbar + ion-content, and wraps
  the content slot in a max-width, horizontally-centred container with side
  padding. Solves the "content glued to the edges on desktop" problem in one place.

  Usage:
    <PageContainer :title="t('home.heading')">
      <template #toolbar-end>        // optional toolbar buttons (slot="end")
        <ion-button>...</ion-button>
      </template>
      <AppCard>...</AppCard>          // page content goes in the default slot
    </PageContainer>

  Props:
    - title?:    header title text (already translated by the caller). Omit to
                 render no header at all (e.g. fully custom pages).
    - maxWidth?: override the content max-width (defaults to --page-max-width).
-->
<template>
  <ion-page>
    <ion-header v-if="title || $slots['toolbar-start'] || $slots['toolbar-end']">
      <ion-toolbar>
        <slot name="toolbar-start" />
        <ion-title v-if="title">{{ title }}</ion-title>
        <ion-buttons v-if="$slots['toolbar-end']" slot="end">
          <slot name="toolbar-end" />
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <div
        class="mx-auto w-full max-w-page box-border px-4 py-6 md:pt-8"
        :style="containerStyle"
      >
        <slot />
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonContent, IonButtons,
} from '@ionic/vue';

const props = defineProps<{
  title?: string;
  maxWidth?: string;
}>();

// maxWidth override still works: it sets max-width inline, winning over max-w-page.
const containerStyle = computed(() =>
  props.maxWidth ? { maxWidth: props.maxWidth } : {},
);
</script>
