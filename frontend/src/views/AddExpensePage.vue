<template>
  <PageContainer :title="t('expenses.addTitle')">
    <template #toolbar-start>
      <ion-buttons slot="start">
        <ion-back-button :default-href="`/groups/${groupId}`" />
      </ion-buttons>
    </template>

    <div v-if="groupLoading" class="flex justify-center py-8">
      <ion-spinner name="crescent" />
    </div>

    <div v-else-if="groupError" class="stack stack-4">
      <AppCard>
        <ion-text color="danger">
          <p class="m-0">{{ groupError }}</p>
        </ion-text>
      </AppCard>
    </div>

    <form v-else @submit.prevent="onSubmit" class="stack stack-6">
      <!-- Basic info -->
      <AppCard>
        <div class="stack stack-4">
          <ion-input
            v-model="title"
            :label="t('expenses.expenseTitle')"
            :placeholder="t('expenses.titlePlaceholder')"
            label-placement="floating"
            fill="outline"
            required
          />

          <ion-input
            v-model="amountStr"
            type="number"
            inputmode="decimal"
            min="0.01"
            step="0.01"
            :label="t('expenses.amount')"
            :placeholder="t('expenses.amountPlaceholder')"
            label-placement="floating"
            fill="outline"
            required
          />

          <div class="payer-select-wrap">
            <ion-select
              v-model="paidBy"
              :label="t('expenses.payer')"
              label-placement="floating"
              fill="outline"
              interface="action-sheet"
            >
              <ion-select-option
                v-for="member in members"
                :key="member.userId"
                :value="member.userId"
              >
                {{ member.displayName }}
              </ion-select-option>
            </ion-select>
          </div>

          <ion-input
            v-model="expenseDate"
            type="date"
            :label="t('expenses.date')"
            label-placement="floating"
            fill="outline"
          />
        </div>
      </AppCard>

      <!-- Split type -->
      <AppCard>
        <SplitTypeSelector v-model="splitType" />
      </AppCard>

      <!-- Participants -->
      <AppCard>
        <div class="stack stack-4">
          <p class="m-0 font-medium">{{ t('expenses.participants') }}</p>

          <div
            v-for="member in members"
            :key="member.userId"
            class="participant-row"
          >
            <ion-checkbox
              :value="member.userId"
              :checked="isParticipant(member.userId)"
              @ion-change="toggleParticipant(member.userId, $event)"
            />
            <span class="participant-name">{{ member.displayName }}</span>

            <!-- PERCENTAGE: per-participant percent field -->
            <ion-input
              v-if="splitType === 'PERCENTAGE' && isParticipant(member.userId)"
              v-model="participantValues[member.userId]"
              type="number"
              inputmode="decimal"
              min="0"
              max="100"
              step="0.01"
              :placeholder="t('expenses.split.percentage')"
              fill="outline"
              class="participant-value-input"
            />

            <!-- AMOUNT: per-participant amount field -->
            <ion-input
              v-else-if="splitType === 'AMOUNT' && isParticipant(member.userId)"
              v-model="participantValues[member.userId]"
              type="number"
              inputmode="decimal"
              min="0"
              step="0.01"
              :placeholder="t('expenses.amount')"
              fill="outline"
              class="participant-value-input"
            />

            <!-- EQUAL: preview share -->
            <span
              v-else-if="splitType === 'EQUAL' && isParticipant(member.userId)"
              class="participant-preview"
            >
              {{ equalSharePreview }}
            </span>
          </div>

          <!-- Running totals / warnings -->
          <div v-if="splitType === 'PERCENTAGE'" class="sum-info">
            <ion-text :color="pctSumOk ? 'medium' : 'warning'">
              {{ t('expenses.percentTotal') }}: {{ pctSum.toFixed(2) }}%
              <span v-if="!pctSumOk"> — {{ t('expenses.percentMismatch') }}</span>
            </ion-text>
          </div>

          <div v-if="splitType === 'AMOUNT'" class="sum-info">
            <ion-text :color="amtSumOk ? 'medium' : 'warning'">
              {{ t('expenses.amountTotal') }}: {{ amtSum.toFixed(2) }}
              <span v-if="!amtSumOk"> — {{ t('expenses.sumMismatch') }}</span>
            </ion-text>
          </div>
        </div>
      </AppCard>

      <!-- Submit error -->
      <ion-text v-if="submitError" color="danger">
        <p class="m-0 px-1">{{ submitError }}</p>
      </ion-text>

      <div class="stack stack-3">
        <ion-button
          expand="block"
          type="submit"
          :disabled="submitting || !canSubmit"
        >
          <ion-spinner v-if="submitting" name="crescent" slot="start" />
          {{ t('expenses.submit') }}
        </ion-button>
        <ion-button expand="block" fill="outline" @click="goBack">
          {{ t('expenses.cancel') }}
        </ion-button>
      </div>
    </form>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {
  IonButtons,
  IonBackButton,
  IonSpinner,
  IonText,
  IonInput,
  IonSelect,
  IonSelectOption,
  IonCheckbox,
  IonButton,
} from '@ionic/vue';
import PageContainer from '@/components/PageContainer.vue';
import AppCard from '@/components/AppCard.vue';
import SplitTypeSelector from '@/components/SplitTypeSelector.vue';
import { useGroupsStore, GroupError } from '@/stores/groups';
import { useExpensesStore, ExpenseError } from '@/stores/expenses';
import type { SplitType } from '@/types/expense';

const props = defineProps<{ id: string }>();

const router = useRouter();
const { t } = useI18n();
const groupsStore = useGroupsStore();
const expensesStore = useExpensesStore();

const groupId = computed(() => props.id);
const members = computed(() => groupsStore.currentGroup?.members ?? []);

// ── Group loading ──────────────────────────────────────────────────────────
const groupLoading = ref(false);
const groupError = ref('');

// ── Form state ─────────────────────────────────────────────────────────────
const title = ref('');
const amountStr = ref('');
const paidBy = ref('');
const expenseDate = ref(todayIso());
const splitType = ref<SplitType>('EQUAL');

// Selected participant IDs
const selectedParticipants = ref<Set<string>>(new Set());
// Per-participant value string (for PERCENTAGE and AMOUNT modes)
const participantValues = reactive<Record<string, string>>({});

const submitting = ref(false);
const submitError = ref('');

// ── Helpers ────────────────────────────────────────────────────────────────
function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Decode JWT payload to extract sub (userId). No signature verification — purely for UX. */
function currentUserId(): string | null {
  const token = localStorage.getItem('splitit-token');
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub ?? null;
  } catch {
    return null;
  }
}

function isParticipant(userId: string): boolean {
  return selectedParticipants.value.has(userId);
}

function toggleParticipant(userId: string, event: CustomEvent): void {
  if (event.detail.checked) {
    selectedParticipants.value.add(userId);
  } else {
    selectedParticipants.value.delete(userId);
    delete participantValues[userId];
  }
}

// ── Derived ────────────────────────────────────────────────────────────────
const amount = computed(() => {
  const n = parseFloat(amountStr.value);
  return isNaN(n) ? 0 : n;
});

const activeParticipants = computed(() =>
  [...selectedParticipants.value],
);

const equalSharePreview = computed(() => {
  const n = activeParticipants.value.length;
  if (n === 0 || amount.value === 0) return '';
  return (amount.value / n).toFixed(2);
});

const pctSum = computed(() =>
  activeParticipants.value.reduce((sum, uid) => {
    const v = parseFloat(participantValues[uid] ?? '0');
    return sum + (isNaN(v) ? 0 : v);
  }, 0),
);

const pctSumOk = computed(() => Math.abs(pctSum.value - 100) < 0.001);

const amtSum = computed(() =>
  activeParticipants.value.reduce((sum, uid) => {
    const v = parseFloat(participantValues[uid] ?? '0');
    return sum + (isNaN(v) ? 0 : v);
  }, 0),
);

const amtSumOk = computed(() => Math.abs(amtSum.value - amount.value) < 0.005);

const canSubmit = computed(() => {
  if (!title.value.trim()) return false;
  if (amount.value <= 0) return false;
  if (!paidBy.value) return false;
  if (activeParticipants.value.length === 0) return false;
  if (splitType.value === 'PERCENTAGE' && !pctSumOk.value) return false;
  if (splitType.value === 'AMOUNT' && !amtSumOk.value) return false;
  return true;
});

// ── Lifecycle ──────────────────────────────────────────────────────────────
onMounted(async () => {
  groupLoading.value = true;
  groupError.value = '';
  try {
    await groupsStore.fetchGroup(groupId.value);

    // Default paidBy = current user, fallback = first member
    const uid = currentUserId();
    const memberIds = members.value.map((m) => m.userId);
    paidBy.value = (uid && memberIds.includes(uid)) ? uid : (memberIds[0] ?? '');

    // Default: all members participate
    selectedParticipants.value = new Set(memberIds);
  } catch (err) {
    if (err instanceof GroupError && err.status === 404) {
      groupError.value = t('groups.notFound');
    } else {
      groupError.value = t('expenses.loadError');
    }
  } finally {
    groupLoading.value = false;
  }
});

// ── Actions ────────────────────────────────────────────────────────────────
async function onSubmit(): Promise<void> {
  submitError.value = '';
  submitting.value = true;

  const participants = activeParticipants.value.map((uid) => {
    if (splitType.value === 'EQUAL') {
      return { userId: uid };
    }
    const raw = participantValues[uid];
    return { userId: uid, value: raw !== undefined && raw !== '' ? raw : undefined };
  });

  try {
    await expensesStore.addExpense(groupId.value, {
      title: title.value.trim(),
      amount: amountStr.value,
      paidBy: paidBy.value,
      splitType: splitType.value,
      expenseDate: expenseDate.value,
      participants,
    });
    // Navigate back and refresh
    await expensesStore.fetchExpenses(groupId.value);
    await expensesStore.fetchBalance(groupId.value);
    router.push({ name: 'group-detail', params: { id: groupId.value } });
  } catch (err) {
    if (err instanceof ExpenseError) {
      const detail = err.errors.length > 0 ? err.errors.join('; ') : err.message;
      submitError.value = detail || t('expenses.saveError');
    } else {
      submitError.value = t('expenses.saveError');
    }
  } finally {
    submitting.value = false;
  }
}

function goBack(): void {
  router.push({ name: 'group-detail', params: { id: groupId.value } });
}
</script>

<style scoped>
.participant-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.participant-name {
  flex: 1;
  font-size: var(--font-size-base);
}

.participant-value-input {
  width: 110px;
  flex-shrink: 0;
}

.participant-preview {
  font-size: var(--font-size-sm);
  color: var(--ion-color-medium);
  min-width: 60px;
  text-align: right;
}

.sum-info {
  font-size: var(--font-size-sm);
}

.payer-select-wrap {
  /* ion-select fill="outline" needs the wrapper to control height */
}
</style>
