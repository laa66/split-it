<template>
  <PageContainer :title="t('settlements.title')">
    <template #toolbar-start>
      <ion-buttons slot="start">
        <ion-back-button :default-href="`/groups/${groupId}`" />
      </ion-buttons>
    </template>

    <div v-if="store.loading" class="flex justify-center py-8">
      <ion-spinner name="crescent" />
    </div>

    <div v-else-if="store.error" class="stack stack-4">
      <AppCard>
        <ion-text color="danger">
          <p class="m-0">{{ t('settlements.loadError') }}</p>
        </ion-text>
      </AppCard>
    </div>

    <div v-else-if="store.plan.length === 0" class="stack stack-4">
      <AppCard>
        <p class="m-0 text-medium text-sm">{{ t('settlements.allSettled') }}</p>
      </AppCard>
    </div>

    <AppCard v-else padding="0">
      <ion-list lines="full">
        <SettlementItem
          v-for="item in store.plan"
          :key="`${item.payerId}-${item.payeeId}`"
          :suggestion="item"
          :pending="pendingKey === `${item.payerId}-${item.payeeId}`"
          @mark-paid="onMarkPaid(item)"
        />
      </ion-list>
    </AppCard>

    <ion-toast
      :is-open="toastOpen"
      :message="toastMessage"
      :duration="3000"
      color="danger"
      @did-dismiss="toastOpen = false"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  IonButtons, IonBackButton, IonSpinner, IonText, IonList, IonToast,
} from '@ionic/vue';
import PageContainer from '@/components/PageContainer.vue';
import AppCard from '@/components/AppCard.vue';
import SettlementItem from '@/components/SettlementItem.vue';
import { useSettlementsStore } from '@/stores/settlements';
import type { SettlementSuggestion } from '@/types/settlement';

const props = defineProps<{ id: string }>();

const { t } = useI18n();
const store = useSettlementsStore();

const groupId = props.id;
const pendingKey = ref<string | null>(null);
const toastOpen = ref(false);
const toastMessage = ref('');

async function onMarkPaid(suggestion: SettlementSuggestion): Promise<void> {
  const key = `${suggestion.payerId}-${suggestion.payeeId}`;
  pendingKey.value = key;
  try {
    await store.markPaid(groupId, suggestion);
  } catch {
    toastMessage.value = t('settlements.markPaidError');
    toastOpen.value = true;
  } finally {
    pendingKey.value = null;
  }
}

onMounted(() => store.fetchPlan(groupId));
</script>
