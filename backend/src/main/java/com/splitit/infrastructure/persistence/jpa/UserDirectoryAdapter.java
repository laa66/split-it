package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.group.model.DirectoryUser;
import com.splitit.domain.group.port.out.UserDirectory;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Bridges the group domain's UserDirectory port to the existing users table, reusing the
 * user persistence layer without exposing the user domain model.
 */
@Component
public class UserDirectoryAdapter implements UserDirectory {

    private final SpringDataUserRepository userRepository;

    public UserDirectoryAdapter(SpringDataUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<DirectoryUser> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(u -> new DirectoryUser(u.getId(), u.getEmail(), u.getDisplayName()));
    }
}
