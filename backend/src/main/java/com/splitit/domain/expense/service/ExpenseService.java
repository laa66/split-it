package com.splitit.domain.expense.service;

import com.splitit.domain.expense.exception.ExpenseAccessDeniedException;
import com.splitit.domain.expense.exception.ExpenseNotFoundException;
import com.splitit.domain.expense.exception.InvalidSplitException;
import com.splitit.domain.expense.model.Expense;
import com.splitit.domain.expense.model.ExpenseShare;
import com.splitit.domain.expense.model.ExpenseWithShares;
import com.splitit.domain.expense.model.MemberBalance;
import com.splitit.domain.expense.model.SplitType;
import com.splitit.domain.expense.port.in.CalculateBalanceUseCase;
import com.splitit.domain.expense.port.in.ManageExpensesUseCase;
import com.splitit.domain.expense.port.out.ExpenseRepository;
import com.splitit.domain.expense.port.out.GroupMembershipPort;
import com.splitit.domain.group.exception.GroupNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pure domain service — no Spring annotations.
 * Split logic guarantees Σ(shares) == amount to the cent via penny distribution.
 * Authorization is enforced here (not just in the controller).
 */
public class ExpenseService implements ManageExpensesUseCase, CalculateBalanceUseCase {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final ExpenseRepository expenseRepository;
    private final GroupMembershipPort groupMembershipPort;

    public ExpenseService(ExpenseRepository expenseRepository,
                          GroupMembershipPort groupMembershipPort) {
        this.expenseRepository = expenseRepository;
        this.groupMembershipPort = groupMembershipPort;
    }

    @Override
    public ExpenseWithShares addExpense(UUID callerId, UUID groupId, AddExpenseCommand command) {
        requireMember(groupId, callerId);
        validateExpenseInput(groupId, command);

        List<ExpenseShare> shares = calculateShares(command.splitType(), command.amount(),
                command.participants());

        Expense expense = Expense.newExpense(groupId, command.paidBy(), command.title(),
                command.amount(), command.splitType(), command.expenseDate());
        return expenseRepository.save(expense, shares);
    }

    @Override
    public ExpensePage listExpenses(UUID callerId, UUID groupId, int page, int size) {
        requireMember(groupId, callerId);

        ExpenseRepository.ExpensePage dbPage = expenseRepository.findByGroup(groupId, page, size);
        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) dbPage.totalElements() / size);

        return new ExpensePage(dbPage.content(), page, size, dbPage.totalElements(), totalPages);
    }

    @Override
    public void deleteExpense(UUID callerId, UUID expenseId) {
        ExpenseWithShares ews = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));

        Expense expense = ews.getExpense();

        // Non-member sees 404 (same as non-existent)
        if (!groupMembershipPort.isMember(expense.getGroupId(), callerId)) {
            throw new ExpenseNotFoundException(expenseId);
        }

        boolean isCreator = expense.getPaidBy().equals(callerId);
        boolean isOwner = callerId.equals(groupMembershipPort.findGroupOwnerId(expense.getGroupId()));

        if (!isCreator && !isOwner) {
            throw new ExpenseAccessDeniedException();
        }

        expenseRepository.delete(expenseId);
    }

    @Override
    public List<MemberBalance> balance(UUID callerId, UUID groupId) {
        requireMember(groupId, callerId);
        return expenseRepository.calculateBalances(groupId);
    }

    // -------------------------------------------------------------------------
    // Private: split calculation
    // -------------------------------------------------------------------------

    private List<ExpenseShare> calculateShares(SplitType splitType, BigDecimal amount,
                                               List<ParticipantShare> participants) {
        return switch (splitType) {
            case EQUAL -> calculateEqual(amount, participants);
            case PERCENTAGE -> calculatePercentage(amount, participants);
            case AMOUNT -> calculateAmount(amount, participants);
        };
    }

    private List<ExpenseShare> calculateEqual(BigDecimal amount, List<ParticipantShare> participants) {
        int n = participants.size();
        // base per person, rounded down to 2 decimal places
        BigDecimal base = amount.divide(BigDecimal.valueOf(n), 2, RoundingMode.FLOOR);
        BigDecimal distributed = base.multiply(BigDecimal.valueOf(n));
        // remainder in cents (e.g. 0.01 units)
        BigDecimal remainder = amount.subtract(distributed);
        long pennies = remainder.movePointRight(2).longValue();

        List<ExpenseShare> shares = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BigDecimal share = (i < pennies) ? base.add(new BigDecimal("0.01")) : base;
            shares.add(new ExpenseShare(participants.get(i).userId(), share.setScale(2)));
        }
        return shares;
    }

    private List<ExpenseShare> calculatePercentage(BigDecimal amount,
                                                   List<ParticipantShare> participants) {
        BigDecimal totalPct = participants.stream()
                .map(p -> p.value().setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPct.compareTo(HUNDRED) != 0) {
            throw new InvalidSplitException(
                    "Percentages must sum to 100.00, got " + totalPct.toPlainString());
        }

        // Compute raw amounts (may not sum exactly due to rounding)
        List<BigDecimal> rawAmounts = participants.stream()
                .map(p -> amount.multiply(p.value().setScale(2, RoundingMode.HALF_UP))
                        .divide(HUNDRED, 2, RoundingMode.FLOOR))
                .toList();

        BigDecimal distributed = rawAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainder = amount.subtract(distributed);
        long pennies = remainder.movePointRight(2).longValue();

        List<ExpenseShare> shares = new ArrayList<>(participants.size());
        for (int i = 0; i < participants.size(); i++) {
            BigDecimal share = (i < pennies)
                    ? rawAmounts.get(i).add(new BigDecimal("0.01"))
                    : rawAmounts.get(i);
            shares.add(new ExpenseShare(participants.get(i).userId(), share.setScale(2)));
        }
        return shares;
    }

    private List<ExpenseShare> calculateAmount(BigDecimal amount, List<ParticipantShare> participants) {
        BigDecimal total = participants.stream()
                .map(p -> p.value().setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(amount.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new InvalidSplitException(
                    "Share amounts must sum to " + amount.toPlainString()
                    + ", got " + total.toPlainString());
        }

        return participants.stream()
                .map(p -> new ExpenseShare(p.userId(), p.value().setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private: validation
    // -------------------------------------------------------------------------

    private void validateExpenseInput(UUID groupId, AddExpenseCommand command) {
        if (command.title() == null || command.title().isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (command.title().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Title must be at most " + MAX_TITLE_LENGTH + " characters");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (command.expenseDate() == null) {
            throw new IllegalArgumentException("Expense date must not be null");
        }
        if (command.participants() == null || command.participants().isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required");
        }

        Set<UUID> seen = new HashSet<>();
        for (ParticipantShare ps : command.participants()) {
            if (!seen.add(ps.userId())) {
                throw new IllegalArgumentException("Duplicate participant: " + ps.userId());
            }
        }

        // paidBy must be a member
        if (!groupMembershipPort.isMember(groupId, command.paidBy())) {
            throw new GroupNotFoundException(groupId);
        }

        // All participants must be members
        List<UUID> memberIds = groupMembershipPort.listMemberIds(groupId);
        Set<UUID> memberSet = new HashSet<>(memberIds);
        for (ParticipantShare ps : command.participants()) {
            if (!memberSet.contains(ps.userId())) {
                throw new IllegalArgumentException("Participant " + ps.userId() + " is not a group member");
            }
        }
    }

    private void requireMember(UUID groupId, UUID userId) {
        if (!groupMembershipPort.isMember(groupId, userId)) {
            throw new GroupNotFoundException(groupId);
        }
    }
}
