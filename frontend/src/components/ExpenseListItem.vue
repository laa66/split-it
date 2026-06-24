<template>
  <ion-item lines="none">
    <ion-label>
      <h3>{{ expense.title }}</h3>
      <p>{{ expense.expenseDate }} · {{ expense.amount }}</p>
      <p>{{ t('expenses.paidBy', { name: payerName }) }}</p>
    </ion-label>
    <ion-button
      slot="end"
      fill="clear"
      color="danger"
      size="small"
      @click="emit('delete', expense.id)"
    >
      {{ t('expenses.delete') }}
    </ion-button>
  </ion-item>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { IonItem, IonLabel, IonButton } from '@ionic/vue';
import type { Expense } from '@/types/expense';
import type { GroupMember } from '@/types/group';

const props = defineProps<{
  expense: Expense;
  members: GroupMember[];
}>();

const emit = defineEmits<{
  delete: [expenseId: string];
}>();

const { t } = useI18n();

const payerName = computed(() => {
  const member = props.members.find((m) => m.userId === props.expense.paidBy);
  return member?.displayName ?? props.expense.paidBy;
});
</script>
