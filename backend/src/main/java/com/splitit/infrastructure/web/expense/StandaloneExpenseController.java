package com.splitit.infrastructure.web.expense;

import com.splitit.domain.expense.port.in.ManageExpensesUseCase;
import com.splitit.infrastructure.security.AuthenticatedUser;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
public class StandaloneExpenseController {

    private static final Logger log = LoggerFactory.getLogger(StandaloneExpenseController.class);

    private final ManageExpensesUseCase manageExpensesUseCase;

    public StandaloneExpenseController(ManageExpensesUseCase manageExpensesUseCase) {
        this.manageExpensesUseCase = manageExpensesUseCase;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteExpense(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        manageExpensesUseCase.deleteExpense(user.id(), id);
        log.info("Expense deleted: id={} by user={}", id, user.id());
    }
}
