<template>
  <PageContainer :title="t('app.title')">
    <template #toolbar-end>
      <ion-button @click="switchLocale('pl')">PL</ion-button>
      <ion-button @click="switchLocale('en')">EN</ion-button>
      <ion-button @click="showCreateModal = true">{{ t('groups.create') }}</ion-button>
      <ion-button @click="onLogout">{{ t('auth.logout') }}</ion-button>
    </template>

    <div class="stack stack-6">
      <h1 class="m-0">{{ t('groups.title') }}</h1>

      <div v-if="groupsStore.loading" class="flex items-center justify-between">
        <ion-spinner name="crescent" />
      </div>

      <template v-else-if="groupsStore.hasGroups">
        <div class="stack stack-3">
          <GroupListItem
            v-for="group in groupsStore.groups"
            :key="group.id"
            :group="group"
            @open="router.push({ name: 'group-detail', params: { id: group.id } })"
          />
        </div>
      </template>

      <template v-else>
        <AppCard>
          <div class="stack stack-4 text-center">
            <p class="m-0 text-medium">{{ t('groups.empty') }}</p>
            <ion-button @click="showCreateModal = true">{{ t('groups.create') }}</ion-button>
          </div>
        </AppCard>
      </template>
    </div>

    <CreateGroupModal
      :is-open="showCreateModal"
      @close="showCreateModal = false"
      @created="onGroupCreated"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { IonButton, IonSpinner } from '@ionic/vue';
import PageContainer from '@/components/PageContainer.vue';
import AppCard from '@/components/AppCard.vue';
import GroupListItem from '@/components/GroupListItem.vue';
import CreateGroupModal from '@/components/CreateGroupModal.vue';
import { setLocale, type AppLocale } from '@/i18n';
import { useAuthStore } from '@/stores/auth';
import { useGroupsStore } from '@/stores/groups';
import type { GroupDetails } from '@/types/group';

const { t } = useI18n();
const router = useRouter();
const auth = useAuthStore();
const groupsStore = useGroupsStore();
const showCreateModal = ref(false);

function switchLocale(locale: AppLocale) {
  setLocale(locale);
}

function onLogout() {
  auth.logout();
  router.push('/login');
}

function onGroupCreated(group: GroupDetails) {
  showCreateModal.value = false;
  router.push({ name: 'group-detail', params: { id: group.id } });
}

onMounted(() => {
  groupsStore.fetchGroups();
});
</script>
