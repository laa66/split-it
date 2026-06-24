import { defineStore } from 'pinia';
import { ref } from 'vue';
import axios from 'axios';
import * as expensesApi from '@/api/expenses';
import type { ApiError } from '@/types/auth';
import type { AddExpensePayload, Expense, MemberBalance } from '@/types/expense';

export class ExpenseError extends Error {
  status: number;
  errors: string[];

  constructor(message: string, status: number, errors: string[] = []) {
    super(message);
    this.name = 'ExpenseError';
    this.status = status;
    this.errors = errors;
  }
}

function toExpenseError(err: unknown): ExpenseError {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiError | undefined;
    const status = err.response?.status ?? 0;
    const message = data?.message ?? err.message;
    return new ExpenseError(message, status, data?.errors ?? []);
  }
  return new ExpenseError('Unexpected error', 0);
}

export const useExpensesStore = defineStore('expenses', () => {
  const expenses = ref<Expense[]>([]);
  const currentPage = ref(0);
  const pageSize = ref(20);
  const totalElements = ref(0);
  const totalPages = ref(0);
  const balances = ref<MemberBalance[]>([]);
  const loading = ref(false);
  const error = ref<ExpenseError | null>(null);

  async function fetchExpenses(groupId: string, page = 0): Promise<void> {
    loading.value = true;
    error.value = null;
    try {
      const result = await expensesApi.listExpenses(groupId, page, pageSize.value);
      expenses.value = result.content;
      currentPage.value = result.page;
      totalElements.value = result.totalElements;
      totalPages.value = result.totalPages;
    } catch (err) {
      error.value = toExpenseError(err);
      throw error.value;
    } finally {
      loading.value = false;
    }
  }

  async function addExpense(groupId: string, payload: AddExpensePayload): Promise<Expense> {
    try {
      const created = await expensesApi.addExpense(groupId, payload);
      // Prepend to list so new expense appears first.
      expenses.value = [created, ...expenses.value];
      totalElements.value += 1;
      return created;
    } catch (err) {
      throw toExpenseError(err);
    }
  }

  async function removeExpense(expenseId: string, groupId: string): Promise<void> {
    try {
      await expensesApi.deleteExpense(expenseId);
      expenses.value = expenses.value.filter((e) => e.id !== expenseId);
      totalElements.value = Math.max(0, totalElements.value - 1);
      // Refresh balances since they change after deletion.
      await fetchBalance(groupId);
    } catch (err) {
      throw toExpenseError(err);
    }
  }

  async function fetchBalance(groupId: string): Promise<void> {
    try {
      balances.value = await expensesApi.getBalance(groupId);
    } catch (err) {
      throw toExpenseError(err);
    }
  }

  return {
    expenses,
    currentPage,
    pageSize,
    totalElements,
    totalPages,
    balances,
    loading,
    error,
    fetchExpenses,
    addExpense,
    removeExpense,
    fetchBalance,
  };
});
