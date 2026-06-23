<template>
  <AppCard padding="var(--space-4)">
    <button
      type="button"
      class="w-full text-left cursor-pointer bg-transparent border-0 p-0"
      @click="emit('open')"
    >
      <div class="stack stack-1">
        <div class="flex items-center justify-between">
          <span class="font-semibold text-base">{{ group.name }}</span>
          <ion-badge :color="group.role === 'OWNER' ? 'primary' : 'medium'">
            {{ group.role === 'OWNER' ? t('groups.role.owner') : t('groups.role.member') }}
          </ion-badge>
        </div>
        <p v-if="group.description" class="m-0 text-medium text-sm">{{ group.description }}</p>
        <div class="flex items-center justify-between text-sm text-medium">
          <span>{{ t('groups.membersCount', { count: group.membersCount }) }}</span>
          <span>{{ t('groups.createdAtLabel') }}: {{ formattedDate }}</span>
        </div>
      </div>
    </button>
  </AppCard>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { IonBadge } from '@ionic/vue';
import AppCard from '@/components/AppCard.vue';
import type { GroupSummary } from '@/types/group';

const props = defineProps<{
  group: GroupSummary;
}>();

const emit = defineEmits<{
  (e: 'open'): void;
}>();

const { t } = useI18n();

const formattedDate = computed(() =>
  new Date(props.group.createdAt).toLocaleDateString(),
);
</script>
