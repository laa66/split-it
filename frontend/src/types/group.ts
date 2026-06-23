export type MemberRole = 'OWNER' | 'MEMBER';

// Mirrors backend GroupSummaryResponse.
export interface GroupSummary {
  id: string;
  name: string;
  description: string | null;
  role: MemberRole;
  membersCount: number;
  createdAt: string;
}

// Mirrors backend GroupDetailsResponse.MemberResponse.
export interface GroupMember {
  userId: string;
  displayName: string;
  email: string;
  role: MemberRole;
}

// Mirrors backend GroupDetailsResponse. Note: no current-user role here (that lives on
// GroupSummary); the detail view carries createdBy + the full member list instead.
export interface GroupDetails {
  id: string;
  name: string;
  description: string | null;
  createdBy: string;
  createdAt: string;
  members: GroupMember[];
}

export interface CreateGroupPayload {
  name: string;
  description?: string;
}

export interface InvitePayload {
  email: string;
}

// Mirrors backend InviteResponse.
export interface InviteResponse {
  addedImmediately: boolean;
  message: string;
}
