<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ t('auth.register') }}</ion-title>
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
              v-model="displayName"
              type="text"
              :label="t('auth.displayName')"
              label-placement="floating"
              autocomplete="name"
              :disabled="loading"
            />
          </ion-item>
          <ion-item>
            <ion-input
              v-model="password"
              type="password"
              :label="t('auth.password')"
              label-placement="floating"
              autocomplete="new-password"
              :disabled="loading"
            />
          </ion-item>
        </ion-list>

        <ion-text v-if="errorMessage" color="danger">
          <p class="form-error">{{ errorMessage }}</p>
        </ion-text>

        <ul v-if="fieldErrors.length" class="field-errors">
          <ion-text color="danger" v-for="(msg, i) in fieldErrors" :key="i">
            <li>{{ msg }}</li>
          </ion-text>
        </ul>

        <ion-button type="submit" expand="block" :disabled="loading" class="ion-margin-top">
          <ion-spinner v-if="loading" name="crescent" />
          <span v-else>{{ t('auth.submit') }}</span>
        </ion-button>
      </form>

      <ion-text class="cta">
        <p>
          {{ t('auth.haveAccount') }}
          <router-link to="/login">{{ t('auth.loginCta') }}</router-link>
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
const displayName = ref('');
const password = ref('');
const loading = ref(false);
const errorMessage = ref('');
const fieldErrors = ref<string[]>([]);

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD = 8;

function validate(): boolean {
  if (!EMAIL_RE.test(email.value.trim())) {
    errorMessage.value = t('auth.invalidEmail');
    return false;
  }
  if (!displayName.value.trim()) {
    errorMessage.value = t('auth.genericError');
    return false;
  }
  if (password.value.length < MIN_PASSWORD) {
    errorMessage.value = t('auth.passwordTooShort');
    return false;
  }
  return true;
}

async function onSubmit() {
  errorMessage.value = '';
  fieldErrors.value = [];

  if (!validate()) return;

  loading.value = true;
  try {
    await auth.register({
      email: email.value.trim(),
      displayName: displayName.value.trim(),
      password: password.value,
    });
    router.push('/home');
  } catch (err) {
    // On error the form stays populated (scenario 3.6) — we do not reset inputs.
    if (err instanceof AuthError) {
      errorMessage.value = err.status === 409
        ? (err.message || t('auth.emailTaken'))
        : err.message || t('auth.genericError');
      fieldErrors.value = err.errors;
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
.field-errors {
  margin: 8px 4px 0;
  padding-left: 20px;
}
.cta {
  display: block;
  margin-top: 24px;
  text-align: center;
}
</style>
