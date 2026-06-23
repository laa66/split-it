package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.group.model.Group;
import com.splitit.domain.group.port.out.GroupRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GroupRepositoryAdapter implements GroupRepository {

    private final SpringDataGroupRepository jpaRepository;

    public GroupRepositoryAdapter(SpringDataGroupRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Group save(Group group) {
        UUID id = group.getId() != null ? group.getId() : UUID.randomUUID();
        OffsetDateTime createdAt =
                group.getCreatedAt() != null ? group.getCreatedAt() : OffsetDateTime.now();

        GroupJpaEntity entity = new GroupJpaEntity(
                id, group.getName(), group.getDescription(), group.getCreatedBy(), createdAt);

        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Group> findById(UUID groupId) {
        return jpaRepository.findById(groupId).map(this::toDomain);
    }

    private Group toDomain(GroupJpaEntity entity) {
        return new Group(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getCreatedBy(), entity.getCreatedAt());
    }
}
