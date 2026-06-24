package com.splitit.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSettlementRepository extends JpaRepository<SettlementJpaEntity, UUID> {
}
