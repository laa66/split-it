<template>
  <PageContainer :title="t('app.title')">
    <template #toolbar-end>
      <ion-button @click="switchLocale('pl')">PL</ion-button>
      <ion-button @click="switchLocale('en')">EN</ion-button>
      <ion-button @click="onLogout">{{ t('auth.logout') }}</ion-button>
    </template>

    <div class="stack stack-6">
      <header class="stack stack-2">
        <h1>{{ t('home.heading') }}</h1>
        <p class="m-0 text-medium">{{ t('home.scaffold') }}</p>
      </header>

      <AppCard>
        <div class="flex items-center justify-between">
          <span>{{ t('home.apiHealth') }}</span>
          <ion-chip :color="healthColor">{{ health }}</ion-chip>
        </div>
      </AppCard>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { IonButton, IonChip } from '@ionic/vue';
import PageContainer from '@/components/PageContainer.vue';
import AppCard from '@/components/AppCard.vue';
import apiClient from '@/api/client';
import { setLocale, type AppLocale } from '@/i18n';
import { useAuthStore } from '@/stores/auth';

const { t } = useI18n();
const router = useRouter();
const auth = useAuthStore();
const health = ref('...');

const healthColor = computed(() => {
  if (health.value === 'UP') return 'success';
  if (health.value === 'DOWN') return 'danger';
  return 'medium';
});

function switchLocale(locale: AppLocale) {
  setLocale(locale);
}

function onLogout() {
  auth.logout();
  router.push('/login');
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
