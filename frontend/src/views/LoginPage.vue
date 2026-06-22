<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ t('auth.login') }}</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <form @submit.prevent="onSubmit">
        <ion-list>
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

        <ion-button type="submit" expand="block" :disabled="loading" class="ion-margin-top">
          <ion-spinner v-if="loading" name="crescent" />
          <span v-else>{{ t('auth.submit') }}</span>
        </ion-button>
      </form>

      <ion-text class="cta">
        <p>
          {{ t('auth.noAccount') }}
          <router-link to="/register">{{ t('auth.registerCta') }}</router-link>
        </p>
      </ion-text>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonContent,
  IonList, IonItem, IonInput, IonButton, IonText, IonSpinner,
} from '@ionic/vue';
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
.form-error {
  margin: 12px 4px 0;
}
.cta {
  display: block;
  margin-top: 24px;
  text-align: center;
}
</style>
