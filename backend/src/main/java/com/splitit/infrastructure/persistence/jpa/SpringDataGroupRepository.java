package com.splitit.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataGroupRepository extends JpaRepository<GroupJpaEntity, UUID> {

    @Query(value = "SELECT id, name FROM groups ORDER BY name", nativeQuery = true)
    List<GroupIdNameView> findAllIdAndName();
}
