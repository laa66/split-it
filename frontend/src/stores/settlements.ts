import { defineStore } from 'pinia';
import { ref } from 'vue';
import axios from 'axios';
import * as settlementsApi from '@/api/settlements';
import { useExpensesStore } from '@/stores/expenses';
import type { ApiError } from '@/types/auth';
import type { SettlementSuggestion } from '@/types/settlement';

export class SettlementError extends Error {
  status: number;
  errors: string[];

  constructor(message: string, status: number, errors: string[] = []) {
    super(message);
    this.name = 'SettlementError';
    this.status = status;
    this.errors = errors;
  }
}

function toSettlementError(err: unknown): SettlementError {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiError | undefined;
    const status = err.response?.status ?? 0;
    const message = data?.message ?? err.message;
    return new SettlementError(message, status, data?.errors ?? []);
  }
  return new SettlementError('Unexpected error', 0);
}

export const useSettlementsStore = defineStore('settlements', () => {
  const plan = ref<SettlementSuggestion[]>([]);
  const loading = ref(false);
  const error = ref<SettlementError | null>(null);

  async function fetchPlan(groupId: string): Promise<void> {
    loading.value = true;
    error.value = null;
    try {
      plan.value = await settlementsApi.getSettlementPlan(groupId);
    } catch (err) {
      error.value = toSettlementError(err);
      throw error.value;
    } finally {
      loading.value = false;
    }
  }

  async function markPaid(groupId: string, suggestion: SettlementSuggestion): Promise<void> {
    try {
      await settlementsApi.recordSettlement(groupId, {
        payerId: suggestion.payerId,
        payeeId: suggestion.payeeId,
        amount: suggestion.amount,
      });
      await fetchPlan(groupId);
      const expensesStore = useExpensesStore();
      await expensesStore.fetchBalance(groupId);
    } catch (err) {
      throw toSettlementError(err);
    }
  }

  return {
    plan,
    loading,
    error,
    fetchPlan,
    markPaid,
  };
});
