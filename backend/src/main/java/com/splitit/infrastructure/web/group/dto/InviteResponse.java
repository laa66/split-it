package com.splitit.infrastructure.web.group.dto;

/**
 * @param addedImmediately true if the invitee already had an account and was added straight away;
 *                         false if a pending email invitation was created.
 */
public record InviteResponse(boolean addedImmediately, String message) {
}
