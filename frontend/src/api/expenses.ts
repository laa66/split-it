import apiClient from './client';
import type { AddExpensePayload, Expense, ExpensePage, MemberBalance } from '@/types/expense';

export async function addExpense(groupId: string, payload: AddExpensePayload): Promise<Expense> {
  const { data } = await apiClient.post<Expense>(`/groups/${groupId}/expenses`, payload);
  return data;
}

export async function listExpenses(
  groupId: string,
  page = 0,
  size = 20,
): Promise<ExpensePage> {
  const { data } = await apiClient.get<ExpensePage>(`/groups/${groupId}/expenses`, {
    params: { page, size },
  });
  return data;
}

export async function deleteExpense(expenseId: string): Promise<void> {
  await apiClient.delete(`/expenses/${expenseId}`);
}

export async function getBalance(groupId: string): Promise<MemberBalance[]> {
  const { data } = await apiClient.get<MemberBalance[]>(`/groups/${groupId}/balance`);
  return data;
}
