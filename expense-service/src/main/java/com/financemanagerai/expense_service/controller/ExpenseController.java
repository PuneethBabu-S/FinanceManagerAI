package com.financemanagerai.expense_service.controller;

import com.financemanagerai.expense_service.dto.ExpenseRequestDTO;
import com.financemanagerai.expense_service.dto.ExpenseResponseDTO;
import com.financemanagerai.expense_service.entity.Expense;
import com.financemanagerai.expense_service.service.ExpenseService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    private String getRequester(Authentication authentication) {
        return authentication.getName();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    @PostMapping("/add")
    public ExpenseResponseDTO addExpense(@RequestBody ExpenseRequestDTO request,
                                         Authentication authentication) {
        String requester = getRequester(authentication);
        boolean admin = isAdmin(authentication);

        Expense expense = request.toEntity();
        Expense saved = expenseService.addExpense(expense, request.getCategoryId(), requester, admin);

        return ExpenseResponseDTO.from(saved);
    }

    @GetMapping
    public List<ExpenseResponseDTO> getExpenses(Authentication authentication,
                                                @RequestParam(defaultValue = "false") boolean includeInactive) {
        String requester = getRequester(authentication);
        List<Expense> expenses = expenseService.getExpensesForUser(requester, includeInactive);

        return expenses.stream().map(ExpenseResponseDTO::from).toList();
    }

    @DeleteMapping("/{expenseId}")
    public void deactivateExpense(@PathVariable Long expenseId,
                                  Authentication authentication) {
        String requester = getRequester(authentication);
        boolean admin = isAdmin(authentication);
        expenseService.deactivateExpense(expenseId, requester, admin);
    }
}