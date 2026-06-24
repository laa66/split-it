package com.splitit.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataExpenseShareRepository extends JpaRepository<ExpenseShareJpaEntity, UUID> {

    List<ExpenseShareJpaEntity> findByExpenseId(UUID expenseId);

    void deleteByExpenseId(UUID expenseId);
}
