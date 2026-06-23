package com.splitit.domain.group.port.out;

/**
 * Out-port for transactional emails sent by the group domain. Pure interface — the adapter
 * owns templating and transport. Mockable as a @MockBean in tests.
 */
public interface EmailSender {

    /** Invitation to someone without an account yet: contains a registration link with the token. */
    void sendGroupInvitation(String toEmail, String groupName, String registrationLink);

    /** Informational notice to an existing user who was added to a group directly. */
    void sendAddedToGroup(String toEmail, String groupName);
}
