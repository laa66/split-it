<template>
  <ion-modal :is-open="isOpen" class="create-group-modal" @did-dismiss="onCancel">
    <ion-header class="ion-no-border">
      <ion-toolbar>
        <ion-title>{{ t('groups.createTitle') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="onCancel">{{ t('groups.cancel') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <div class="modal-body stack stack-4">
      <ion-item>
        <ion-input
          v-model="name"
          :label="t('groups.name')"
          :placeholder="t('groups.namePlaceholder')"
          label-placement="floating"
          required
        />
      </ion-item>

      <ion-item>
        <ion-textarea
          v-model="description"
          :label="t('groups.descriptionOptional')"
          :placeholder="t('groups.descriptionPlaceholder')"
          label-placement="floating"
          :rows="3"
          auto-grow
        />
      </ion-item>

      <ion-text v-if="errorMessage" color="danger">
        <p class="m-0 text-sm">{{ errorMessage }}</p>
      </ion-text>

      <ion-button
        expand="block"
        :disabled="!name.trim() || submitting"
        @click="onSubmit"
      >
        {{ t('groups.submit') }}
      </ion-button>
    </div>
  </ion-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  IonModal,
  IonHeader,
  IonToolbar,
  IonTitle,
  IonButtons,
  IonButton,
  IonItem,
  IonInput,
  IonTextarea,
  IonText,
} from '@ionic/vue';
import { useGroupsStore } from '@/stores/groups';
import type { GroupDetails } from '@/types/group';

defineProps<{
  isOpen: boolean;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'created', group: GroupDetails): void;
}>();

const { t } = useI18n();
const groupsStore = useGroupsStore();

const name = ref('');
const description = ref('');
const submitting = ref(false);
const errorMessage = ref('');

function resetForm() {
  name.value = '';
  description.value = '';
  errorMessage.value = '';
  submitting.value = false;
}

function onCancel() {
  resetForm();
  emit('close');
}

async function onSubmit() {
  if (!name.value.trim()) return;

  submitting.value = true;
  errorMessage.value = '';

  try {
    const payload = {
      name: name.value.trim(),
      ...(description.value.trim() ? { description: description.value.trim() } : {}),
    };
    const created = await groupsStore.createGroup(payload);
    resetForm();
    emit('created', created);
  } catch (err) {
    errorMessage.value =
      err instanceof Error ? err.message : t('groups.createError');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
/*
 * Modal renderuje się jako wycentrowana karta sizowana do treści (--height: auto).
 * Treść jest zwykłym blokowym <div> (a nie ion-content) — ion-content przy auto-wysokości
 * modala zapada się do zera (jest pozycjonowany absolutnie wewnątrz flex-kolumny modala),
 * przez co pola formularza znikały. Blokowy div rośnie naturalnie do swojej zawartości.
 * Na mobile (< 576px) modal zajmuje pełną szerokość jako bottom-sheet.
 */
ion-modal.create-group-modal::part(content) {
  --width: 480px;
  --max-width: calc(100vw - var(--space-8));
  --height: auto;
  --max-height: 90vh;
  --border-radius: var(--radius-lg);
  overflow: hidden;
}

.modal-body {
  padding: var(--space-5);
  max-height: calc(90vh - 56px);
  overflow-y: auto;
  background: var(--app-surface);
}

/* Pola flush z przyciskiem — usuwamy domyślne wcięcie ion-item. */
.modal-body ion-item {
  --padding-start: 0;
  --padding-end: 0;
  --inner-padding-end: 0;
  --background: transparent;
}

@media (max-width: 575px) {
  ion-modal.create-group-modal::part(content) {
    --width: 100%;
    --max-width: 100%;
    --border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  }
}
</style>
