<template>
  <AppCard>
    <div v-if="balances.length === 0" class="stack stack-2">
      <p class="m-0 text-medium text-sm">{{ t('expenses.settledUp') }}</p>
    </div>
    <ion-list v-else lines="none" class="balance-list">
      <ion-item v-for="b in balances" :key="b.userId">
        <ion-label>
          <h3>{{ b.displayName }}</h3>
        </ion-label>
        <ion-badge
          slot="end"
          :color="Number(b.balance) >= 0 ? 'success' : 'danger'"
        >
          {{ formatBalance(b.balance) }}
        </ion-badge>
      </ion-item>
    </ion-list>
  </AppCard>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { IonList, IonItem, IonLabel, IonBadge } from '@ionic/vue';
import AppCard from '@/components/AppCard.vue';
import type { MemberBalance } from '@/types/expense';

defineProps<{
  balances: MemberBalance[];
}>();

const { t } = useI18n();

function formatBalance(balance: string): string {
  const n = Number(balance);
  const abs = Math.abs(n).toFixed(2);
  return n >= 0 ? `+${abs}` : `-${abs}`;
}
</script>

<style scoped>
.balance-list {
  background: transparent;
}
</style>
