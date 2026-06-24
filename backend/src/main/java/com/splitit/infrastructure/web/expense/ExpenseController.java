package com.splitit.infrastructure.web.expense;

import com.splitit.domain.expense.model.SplitType;
import com.splitit.domain.expense.port.in.CalculateBalanceUseCase;
import com.splitit.domain.expense.port.in.ManageExpensesUseCase;
import com.splitit.domain.expense.port.in.ManageExpensesUseCase.AddExpenseCommand;
import com.splitit.domain.expense.port.in.ManageExpensesUseCase.ParticipantShare;
import com.splitit.infrastructure.security.AuthenticatedUser;
import com.splitit.infrastructure.web.expense.dto.AddExpenseRequest;
import com.splitit.infrastructure.web.expense.dto.ExpensePageResponse;
import com.splitit.infrastructure.web.expense.dto.ExpenseResponse;
import com.splitit.infrastructure.web.expense.dto.MemberBalanceResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class ExpenseController {

    private static final Logger log = LoggerFactory.getLogger(ExpenseController.class);

    private final ManageExpensesUseCase manageExpensesUseCase;
    private final CalculateBalanceUseCase calculateBalanceUseCase;

    public ExpenseController(ManageExpensesUseCase manageExpensesUseCase,
                             CalculateBalanceUseCase calculateBalanceUseCase) {
        this.manageExpensesUseCase = manageExpensesUseCase;
        this.calculateBalanceUseCase = calculateBalanceUseCase;
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ExpenseResponse addExpense(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID groupId,
            @Valid @RequestBody AddExpenseRequest request) {

        List<ParticipantShare> participants = request.participants().stream()
                .map(p -> new ParticipantShare(p.userId(), p.value()))
                .toList();

        AddExpenseCommand command = new AddExpenseCommand(
                request.title(), request.amount(), request.paidBy(),
                request.splitType(), request.expenseDate(), participants);

        ExpenseResponse response = ExpenseResponse.from(
                manageExpensesUseCase.addExpense(user.id(), groupId, command));
        log.info("Expense added: id={} group={} amount={} split={} by user={}",
                response.id(), groupId, request.amount(), request.splitType(), user.id());
        return response;
    }

    @GetMapping("/expenses")
    public ExpensePageResponse listExpenses(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ExpensePageResponse.from(
                manageExpensesUseCase.listExpenses(user.id(), groupId, page, size));
    }

    @GetMapping("/balance")
    public List<MemberBalanceResponse> balance(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID groupId) {

        return calculateBalanceUseCase.balance(user.id(), groupId).stream()
                .map(MemberBalanceResponse::from)
                .toList();
    }
}
