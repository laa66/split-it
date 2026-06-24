import apiClient from './client';
import type { RecordSettlementPayload, Settlement, SettlementSuggestion } from '@/types/settlement';

export async function getSettlementPlan(groupId: string): Promise<SettlementSuggestion[]> {
  const { data } = await apiClient.get<SettlementSuggestion[]>(`/groups/${groupId}/settlements`);
  return data;
}

export async function recordSettlement(
  groupId: string,
  payload: RecordSettlementPayload,
): Promise<Settlement> {
  const { data } = await apiClient.post<Settlement>(`/groups/${groupId}/settlements`, payload);
  return data;
}
