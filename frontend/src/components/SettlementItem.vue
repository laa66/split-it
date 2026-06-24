<template>
  <ion-item lines="none">
    <ion-label>
      <h3>{{ suggestion.payerName }} → {{ suggestion.payeeName }}</h3>
      <p>{{ formatAmount(suggestion.amount) }}</p>
    </ion-label>
    <ion-button
      slot="end"
      fill="clear"
      :disabled="pending"
      @click="emit('mark-paid', suggestion)"
    >
      <ion-spinner v-if="pending" name="crescent" />
      <span v-else>{{ t('settlements.markPaid') }}</span>
    </ion-button>
  </ion-item>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { IonItem, IonLabel, IonButton, IonSpinner } from '@ionic/vue';
import type { SettlementSuggestion } from '@/types/settlement';

defineProps<{
  suggestion: SettlementSuggestion;
  pending?: boolean;
}>();

const emit = defineEmits<{
  'mark-paid': [suggestion: SettlementSuggestion];
}>();

const { t } = useI18n();

function formatAmount(amount: string): string {
  return Number(amount).toFixed(2);
}
</script>
