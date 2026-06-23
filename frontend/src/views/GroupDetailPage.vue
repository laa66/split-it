<template>
  <PageContainer :title="pageTitle">
    <template #toolbar-start>
      <ion-buttons slot="start">
        <ion-back-button default-href="/home" />
      </ion-buttons>
    </template>

    <div v-if="loading" class="flex justify-center py-8">
      <ion-spinner name="crescent" />
    </div>

    <div v-else-if="errorMessage" class="stack stack-4">
      <AppCard>
        <ion-text color="danger">
          <p class="m-0">{{ errorMessage }}</p>
        </ion-text>
      </AppCard>
    </div>

    <div v-else-if="group" class="stack stack-6">
      <AppCard>
        <div class="stack stack-2">
          <p v-if="group.description" class="m-0">{{ group.description }}</p>
          <p class="m-0 text-medium text-sm">
            {{ t('groups.createdAt', { date: formattedDate }) }}
          </p>
        </div>
      </AppCard>

      <section class="stack stack-3">
        <h2 class="m-0 text-lg font-semibold">
          {{ t('groups.members') }} ({{ group.members.length }})
        </h2>
        <AppCard padding="0">
          <ion-list lines="full" class="member-list">
            <ion-item v-for="member in group.members" :key="member.userId">
              <ion-label>
                <h3>{{ member.displayName }}</h3>
                <p>{{ member.email }}</p>
              </ion-label>
              <ion-badge
                slot="end"
                :color="member.role === 'OWNER' ? 'primary' : 'medium'"
              >
                {{ member.role === 'OWNER' ? t('groups.role.owner') : t('groups.role.member') }}
              </ion-badge>
            </ion-item>
          </ion-list>
        </AppCard>
      </section>

      <section class="stack stack-3">
        <h2 class="m-0 text-lg font-semibold">{{ t('groups.inviteTitle') }}</h2>
        <AppCard>
          <form @submit.prevent="onInvite" class="stack stack-4">
            <ion-item class="invite-item">
              <ion-input
                v-model="inviteEmail"
                type="email"
                :label="t('groups.inviteEmail')"
                :placeholder="t('groups.inviteEmailPlaceholder')"
                label-placement="floating"
                autocomplete="email"
                :disabled="inviting"
              />
            </ion-item>

            <ion-text v-if="inviteError" color="danger">
              <p class="m-0 px-1">{{ inviteError }}</p>
            </ion-text>

            <ion-button type="submit" expand="block" :disabled="inviting">
              <ion-spinner v-if="inviting" name="crescent" />
              <span v-else>{{ t('groups.inviteSubmit') }}</span>
            </ion-button>
          </form>
        </AppCard>
      </section>
    </div>

    <ion-toast
      :is-open="toastOpen"
      :message="toastMessage"
      :duration="3000"
      color="success"
      @did-dismiss="toastOpen = false"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {
  IonButtons, IonBackButton, IonSpinner, IonText, IonList, IonItem,
  IonLabel, IonBadge, IonInput, IonButton, IonToast,
} from '@ionic/vue';
import PageContainer from '@/components/PageContainer.vue';
import AppCard from '@/components/AppCard.vue';
import { useGroupsStore, GroupError } from '@/stores/groups';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const route = useRoute();
const { t } = useI18n();
const store = useGroupsStore();

const groupId = computed(() => String(route.params.id));
const group = computed(() => store.currentGroup);
const loading = computed(() => store.loading);

const errorMessage = ref('');
const inviteEmail = ref('');
const inviting = ref(false);
const inviteError = ref('');
const toastOpen = ref(false);
const toastMessage = ref('');

const pageTitle = computed(() => group.value?.name ?? t('groups.title'));

const formattedDate = computed(() => {
  if (!group.value) return '';
  return new Date(group.value.createdAt).toLocaleDateString();
});

async function loadGroup() {
  errorMessage.value = '';
  try {
    await store.fetchGroup(groupId.value);
  } catch (err) {
    if (err instanceof GroupError && err.status === 404) {
      errorMessage.value = t('groups.notFound');
    } else {
      errorMessage.value = t('groups.loadError');
    }
  }
}

async function onInvite() {
  inviteError.value = '';
  const email = inviteEmail.value.trim();

  if (!email || !EMAIL_RE.test(email)) {
    inviteError.value = t('groups.inviteInvalidEmail');
    return;
  }

  inviting.value = true;
  try {
    const result = await store.invite(groupId.value, email);
    inviteEmail.value = '';
    toastMessage.value = result.addedImmediately
      ? t('groups.inviteAddedNow')
      : t('groups.inviteSent');
    toastOpen.value = true;
  } catch (err) {
    if (err instanceof GroupError) {
      inviteError.value = err.message || t('groups.inviteError');
    } else {
      inviteError.value = t('groups.inviteError');
    }
  } finally {
    inviting.value = false;
  }
}

onMounted(loadGroup);
</script>

<style scoped>
.member-list {
  background: transparent;
}

/* Pole zapraszania flush z przyciskiem — usuwamy domyślne wcięcie ion-item. */
.invite-item {
  --padding-start: 0;
  --padding-end: 0;
  --inner-padding-end: 0;
  --background: transparent;
}
</style>
