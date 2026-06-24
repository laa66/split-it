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

      <!-- Expenses section -->
      <section class="stack stack-3">
        <div class="section-header">
          <h2 class="m-0 text-lg font-semibold">
            {{ t('expenses.title') }} ({{ expensesStore.totalElements }})
          </h2>
          <ion-button size="small" @click="goToAddExpense">
            {{ t('expenses.add') }}
          </ion-button>
        </div>

        <div v-if="expensesStore.loading" class="flex justify-center py-4">
          <ion-spinner name="crescent" />
        </div>

        <AppCard v-else-if="expensesStore.expenses.length === 0" padding="4">
          <div class="empty-state">
            <p class="m-0 text-medium text-sm">{{ t('expenses.empty') }}</p>
          </div>
        </AppCard>

        <template v-else>
          <!-- ExpenseListItem will be provided by Noob; rendered per expense here -->
          <AppCard
            v-for="expense in expensesStore.expenses"
            :key="expense.id"
            padding="0"
          >
            <ExpenseListItem
              :expense="expense"
              :members="group.members"
              @delete="onDeleteExpense"
            />
          </AppCard>
        </template>
      </section>

      <!-- Balance section -->
      <section class="stack stack-3">
        <div class="section-header">
          <h2 class="m-0 text-lg font-semibold">{{ t('expenses.balances') }}</h2>
          <ion-button size="small" @click="goToSettlements">
            {{ t('settlements.viewPlan') }}
          </ion-button>
        </div>
        <BalanceCard
          :balances="expensesStore.balances"
        />
      </section>

      <section class="stack stack-3">
        <h2 class="m-0 text-lg font-semibold">{{ t('groups.inviteTitle') }}</h2>
        <AppCard>
          <form @submit.prevent="onInvite" class="stack stack-4">
            <ion-item>
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
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {
  IonButtons, IonBackButton, IonSpinner, IonText, IonList, IonItem,
  IonLabel, IonBadge, IonInput, IonButton, IonToast,
} from '@ionic/vue';
import PageContainer from '@/components/PageContainer.vue';
import AppCard from '@/components/AppCard.vue';
import ExpenseListItem from '@/components/ExpenseListItem.vue';
import BalanceCard from '@/components/BalanceCard.vue';
import { useGroupsStore, GroupError } from '@/stores/groups';
import { useExpensesStore } from '@/stores/expenses';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const store = useGroupsStore();
const expensesStore = useExpensesStore();

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
    // Fetch expenses and balances after group is loaded
    await Promise.all([
      expensesStore.fetchExpenses(groupId.value),
      expensesStore.fetchBalance(groupId.value),
    ]);
  } catch (err) {
    if (err instanceof GroupError && err.status === 404) {
      errorMessage.value = t('groups.notFound');
    } else {
      errorMessage.value = t('groups.loadError');
    }
  }
}

async function onDeleteExpense(expenseId: string): Promise<void> {
  try {
    await expensesStore.removeExpense(expenseId, groupId.value);
  } catch {
    // removeExpense already refreshes balances; ignore UI error silently here
    // (ExpenseListItem may show its own error or user can retry)
  }
}

function goToAddExpense(): void {
  router.push({ name: 'add-expense', params: { id: groupId.value } });
}

function goToSettlements(): void {
  router.push({ name: 'settlements', params: { id: groupId.value } });
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

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 96px;
  text-align: center;
}
</style>
