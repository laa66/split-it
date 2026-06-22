package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.user.model.User;
import com.splitit.domain.user.port.out.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Out-port adapter. Bridges the domain User and the JPA entity, and delegates to Spring Data.
 * id and createdAt are assigned here when missing (the DB also has matching defaults), keeping
 * the returned domain object fully populated without a re-read.
 */
@Component
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository jpaRepository;

    public UserRepositoryAdapter(SpringDataUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
        OffsetDateTime createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : OffsetDateTime.now();

        UserJpaEntity entity = new UserJpaEntity(
                id, user.getEmail(), user.getDisplayName(), user.getPasswordHash(), createdAt);

        return toDomain(jpaRepository.save(entity));
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getPasswordHash(),
                entity.getCreatedAt());
    }
}
