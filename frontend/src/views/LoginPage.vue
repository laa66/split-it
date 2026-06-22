<template>
  <PageContainer :title="t('auth.login')" max-width="420px">
    <AppCard class="mt-6">
      <form @submit.prevent="onSubmit" class="stack stack-4">
        <h2 class="auth-heading">{{ t('auth.login') }}</h2>

        <ion-list class="auth-list" lines="full">
          <ion-item>
            <ion-input
              v-model="email"
              type="email"
              :label="t('auth.email')"
              label-placement="floating"
              autocomplete="email"
              :disabled="loading"
            />
          </ion-item>
          <ion-item>
            <ion-input
              v-model="password"
              type="password"
              :label="t('auth.password')"
              label-placement="floating"
              autocomplete="current-password"
              :disabled="loading"
            />
          </ion-item>
        </ion-list>

        <ion-text v-if="errorMessage" color="danger">
          <p class="form-error">{{ errorMessage }}</p>
        </ion-text>

        <ion-button type="submit" expand="block" :disabled="loading">
          <ion-spinner v-if="loading" name="crescent" />
          <span v-else>{{ t('auth.submit') }}</span>
        </ion-button>
      </form>

      <ion-text class="block mt-6 text-center">
        <p>
          {{ t('auth.noAccount') }}
          <router-link to="/register" style="text-decoration: none;">{{ t('auth.registerCta') }}</router-link>
        </p>
      </ion-text>
    </AppCard>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {
  IonList, IonItem, IonInput, IonButton, IonText, IonSpinner,
} from '@ionic/vue';
import PageContainer from '@/components/PageContainer.vue';
import AppCard from '@/components/AppCard.vue';
import { useAuthStore, AuthError } from '@/stores/auth';

const { t } = useI18n();
const router = useRouter();
const auth = useAuthStore();

const email = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');

async function onSubmit() {
  errorMessage.value = '';

  if (!email.value.trim() || !password.value) {
    errorMessage.value = t('auth.genericError');
    return;
  }

  loading.value = true;
  try {
    await auth.login({ email: email.value.trim(), password: password.value });
    router.push('/home');
  } catch (err) {
    if (err instanceof AuthError) {
      errorMessage.value = err.status === 401
        ? t('auth.invalidCredentials')
        : err.message || t('auth.genericError');
    } else {
      errorMessage.value = t('auth.genericError');
    }
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.auth-heading {
  margin: 0;
}
.auth-list {
  background: transparent;
}
.auth-list ion-item {
  --background: transparent;
  --padding-start: 0;
  --inner-padding-end: 0;
}
.form-error {
  margin: 0 var(--space-1);
}
</style>
