import apiClient from './client';
import type {
  CreateGroupPayload,
  GroupDetails,
  GroupSummary,
  InvitePayload,
  InviteResponse,
} from '@/types/group';

export async function listGroups(): Promise<GroupSummary[]> {
  const { data } = await apiClient.get<GroupSummary[]>('/groups');
  return data;
}

export async function createGroup(payload: CreateGroupPayload): Promise<GroupDetails> {
  const { data } = await apiClient.post<GroupDetails>('/groups', payload);
  return data;
}

export async function getGroup(id: string): Promise<GroupDetails> {
  const { data } = await apiClient.get<GroupDetails>(`/groups/${id}`);
  return data;
}

export async function inviteMember(groupId: string, payload: InvitePayload): Promise<InviteResponse> {
  const { data } = await apiClient.post<InviteResponse>(`/groups/${groupId}/invite`, payload);
  return data;
}
