import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import axios from 'axios';
import * as groupsApi from '@/api/groups';
import type { ApiError } from '@/types/auth';
import type {
  CreateGroupPayload,
  GroupDetails,
  GroupSummary,
  InviteResponse,
} from '@/types/group';

/**
 * Thrown by group store actions so views can render a user-facing message and
 * field-level errors. Wraps the backend error envelope {status, message, errors[]}.
 */
export class GroupError extends Error {
  status: number;
  errors: string[];

  constructor(message: string, status: number, errors: string[] = []) {
    super(message);
    this.name = 'GroupError';
    this.status = status;
    this.errors = errors;
  }
}

function toGroupError(err: unknown): GroupError {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiError | undefined;
    const status = err.response?.status ?? 0;
    const message = data?.message ?? err.message;
    return new GroupError(message, status, data?.errors ?? []);
  }
  return new GroupError('Unexpected error', 0);
}

export const useGroupsStore = defineStore('groups', () => {
  const groups = ref<GroupSummary[]>([]);
  const currentGroup = ref<GroupDetails | null>(null);
  const loading = ref(false);
  const error = ref<GroupError | null>(null);

  const hasGroups = computed(() => groups.value.length > 0);

  async function fetchGroups(): Promise<void> {
    loading.value = true;
    error.value = null;
    try {
      groups.value = await groupsApi.listGroups();
    } catch (err) {
      error.value = toGroupError(err);
      throw error.value;
    } finally {
      loading.value = false;
    }
  }

  async function createGroup(payload: CreateGroupPayload): Promise<GroupDetails> {
    try {
      const created = await groupsApi.createGroup(payload);
      // Reflect the new group in the list immediately, without a refetch round-trip.
      groups.value = [
        {
          id: created.id,
          name: created.name,
          description: created.description,
          role: 'OWNER',
          membersCount: created.members.length,
          createdAt: created.createdAt,
        },
        ...groups.value,
      ];
      return created;
    } catch (err) {
      throw toGroupError(err);
    }
  }

  async function fetchGroup(id: string): Promise<void> {
    loading.value = true;
    error.value = null;
    currentGroup.value = null;
    try {
      currentGroup.value = await groupsApi.getGroup(id);
    } catch (err) {
      error.value = toGroupError(err);
      throw error.value;
    } finally {
      loading.value = false;
    }
  }

  async function invite(groupId: string, email: string): Promise<InviteResponse> {
    try {
      const result = await groupsApi.inviteMember(groupId, { email });
      // If the invitee already had an account they were added now — refresh the member
      // list so the new member appears without a manual reload.
      if (result.addedImmediately && currentGroup.value?.id === groupId) {
        await fetchGroup(groupId);
      }
      return result;
    } catch (err) {
      throw toGroupError(err);
    }
  }

  return {
    groups,
    currentGroup,
    loading,
    error,
    hasGroups,
    fetchGroups,
    createGroup,
    fetchGroup,
    invite,
  };
});
