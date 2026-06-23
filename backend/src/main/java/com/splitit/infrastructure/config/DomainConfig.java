package com.splitit.infrastructure.config;

import com.splitit.domain.group.port.out.EmailSender;
import com.splitit.domain.group.port.out.GroupMemberRepository;
import com.splitit.domain.group.port.out.GroupRepository;
import com.splitit.domain.group.port.out.InvitationRepository;
import com.splitit.domain.group.port.out.UserDirectory;
import com.splitit.domain.group.service.GroupService;
import com.splitit.domain.group.service.InvitationService;
import com.splitit.domain.user.port.out.PasswordHasher;
import com.splitit.domain.user.port.out.UserRepository;
import com.splitit.domain.user.service.UserService;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires pure domain services (which carry no Spring annotations) as beans,
 * injecting their out-port adapters.
 */
@Configuration
public class DomainConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public UserService userService(UserRepository userRepository, PasswordHasher passwordHasher) {
        return new UserService(userRepository, passwordHasher);
    }

    @Bean
    public GroupService groupService(GroupRepository groupRepository,
                                     GroupMemberRepository groupMemberRepository) {
        return new GroupService(groupRepository, groupMemberRepository);
    }

    @Bean
    public InvitationService invitationService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            InvitationRepository invitationRepository,
            UserDirectory userDirectory,
            EmailSender emailSender,
            Clock clock,
            @Value("${app.invitation.expiration-days:7}") long invitationExpirationDays,
            @Value("${app.base-url}") String baseUrl) {
        return new InvitationService(groupRepository, groupMemberRepository, invitationRepository,
                userDirectory, emailSender, Duration.ofDays(invitationExpirationDays), baseUrl, clock);
    }
}
