package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.settlement.model.Settlement;
import com.splitit.domain.settlement.port.out.SettlementRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SettlementRepositoryAdapter implements SettlementRepository {

    private final SpringDataSettlementRepository jpaRepository;

    public SettlementRepositoryAdapter(SpringDataSettlementRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Settlement save(Settlement settlement) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        UUID id = UUID.randomUUID();

        SettlementJpaEntity entity = new SettlementJpaEntity(
                id,
                settlement.getGroupId(),
                settlement.getPayerId(),
                settlement.getPayeeId(),
                settlement.getAmount(),
                "CONFIRMED",
                true,
                true,
                settlement.getSettledAt() != null ? settlement.getSettledAt() : now,
                now
        );

        SettlementJpaEntity saved = jpaRepository.save(entity);

        return new Settlement(
                saved.getId(),
                saved.getGroupId(),
                saved.getPayerId(),
                saved.getPayeeId(),
                saved.getAmount(),
                saved.getStatus(),
                saved.getSettledAt(),
                saved.getCreatedAt()
        );
    }
}
