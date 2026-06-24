<template>
  <div class="stack stack-2">
    <p class="m-0 text-sm font-medium">{{ t('expenses.split.label') }}</p>
    <ion-segment :value="modelValue" @ion-change="onChange">
      <ion-segment-button value="EQUAL">
        <ion-label>{{ t('expenses.split.equal') }}</ion-label>
      </ion-segment-button>
      <ion-segment-button value="PERCENTAGE">
        <ion-label>{{ t('expenses.split.percentage') }}</ion-label>
      </ion-segment-button>
      <ion-segment-button value="AMOUNT">
        <ion-label>{{ t('expenses.split.amount') }}</ion-label>
      </ion-segment-button>
    </ion-segment>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { IonSegment, IonSegmentButton, IonLabel } from '@ionic/vue';
import type { SplitType } from '@/types/expense';

const props = defineProps<{ modelValue: SplitType }>();
const emit = defineEmits<{ 'update:modelValue': [value: SplitType] }>();

const { t } = useI18n();

function onChange(event: CustomEvent) {
  const val = event.detail.value as SplitType;
  if (val) emit('update:modelValue', val);
}
</script>
