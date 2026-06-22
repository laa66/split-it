<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ t('app.title') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="switchLocale('pl')">PL</ion-button>
          <ion-button @click="switchLocale('en')">EN</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <h1>{{ t('home.heading') }}</h1>
      <p>{{ t('home.scaffold') }}</p>
      <p>API health: <strong>{{ health }}</strong></p>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonContent,
  IonButtons, IonButton,
} from '@ionic/vue';
import apiClient from '@/api/client';
import { setLocale, type AppLocale } from '@/i18n';

const { t } = useI18n();
const health = ref('...');

function switchLocale(locale: AppLocale) {
  setLocale(locale);
}

onMounted(async () => {
  try {
    const { data } = await apiClient.get('/health');
    health.value = data.status;
  } catch {
    health.value = 'DOWN';
  }
});
</script>
