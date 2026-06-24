package com.splitit.infrastructure.persistence.jpa;

import com.splitit.domain.expense.model.Expense;
import com.splitit.domain.expense.model.ExpenseShare;
import com.splitit.domain.expense.model.ExpenseWithShares;
import com.splitit.domain.expense.model.MemberBalance;
import com.splitit.domain.expense.model.SplitType;
import com.splitit.domain.expense.port.out.ExpenseRepository;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ExpenseRepositoryAdapter implements ExpenseRepository {

    private final SpringDataExpenseRepository expenseRepo;
    private final SpringDataExpenseShareRepository shareRepo;

    public ExpenseRepositoryAdapter(SpringDataExpenseRepository expenseRepo,
                                    SpringDataExpenseShareRepository shareRepo) {
        this.expenseRepo = expenseRepo;
        this.shareRepo = shareRepo;
    }

    @Override
    public ExpenseWithShares save(Expense expense, List<ExpenseShare> shares) {
        UUID id = UUID.randomUUID();
        ExpenseJpaEntity entity = new ExpenseJpaEntity(
                id,
                expense.getGroupId(),
                expense.getPaidBy(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getSplitType().name(),
                expense.getExpenseDate(),
                java.time.OffsetDateTime.now(ZoneOffset.UTC)
        );
        ExpenseJpaEntity saved = expenseRepo.save(entity);

        List<ExpenseShareJpaEntity> shareEntities = shares.stream()
                .map(s -> new ExpenseShareJpaEntity(UUID.randomUUID(), saved.getId(),
                        s.getUserId(), s.getShareAmount()))
                .toList();
        shareRepo.saveAll(shareEntities);

        return toAggregate(saved, shareEntities);
    }

    @Override
    public Optional<ExpenseWithShares> findById(UUID expenseId) {
        return expenseRepo.findById(expenseId)
                .map(e -> {
                    List<ExpenseShareJpaEntity> shareEntities = shareRepo.findByExpenseId(expenseId);
                    return toAggregate(e, shareEntities);
                });
    }

    @Override
    public ExpensePage findByGroup(UUID groupId, int page, int size) {
        long total = expenseRepo.countByGroupId(groupId);
        long offset = (long) page * size;
        List<ExpenseJpaEntity> entities = expenseRepo.findByGroupIdPaged(groupId, size, offset);

        List<ExpenseWithShares> content = entities.stream()
                .map(e -> {
                    List<ExpenseShareJpaEntity> shareEntities = shareRepo.findByExpenseId(e.getId());
                    return toAggregate(e, shareEntities);
                })
                .toList();

        return new ExpensePage(content, total);
    }

    @Override
    public void delete(UUID expenseId) {
        shareRepo.deleteByExpenseId(expenseId);
        expenseRepo.deleteById(expenseId);
    }

    @Override
    public List<MemberBalance> calculateBalances(UUID groupId) {
        return expenseRepo.calculateBalances(groupId).stream()
                .map(v -> new MemberBalance(v.getUserId(), v.getDisplayName(), v.getBalance()))
                .toList();
    }

    private ExpenseWithShares toAggregate(ExpenseJpaEntity e,
                                          List<ExpenseShareJpaEntity> shareEntities) {
        Expense expense = new Expense(
                e.getId(), e.getGroupId(), e.getPaidBy(), e.getTitle(), e.getAmount(),
                SplitType.valueOf(e.getSplitType()), e.getExpenseDate(),
                e.getCreatedAt()
        );
        List<ExpenseShare> shares = shareEntities.stream()
                .map(s -> new ExpenseShare(s.getUserId(), s.getShareAmount()))
                .toList();
        return new ExpenseWithShares(expense, shares);
    }
}
