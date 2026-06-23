package com.splitit.domain.group.model;

/** Role of a user within a group. The creator is OWNER; invited users join as MEMBER. */
public enum MemberRole {
    OWNER,
    MEMBER
}
