package com.financemanagerai.expense_service.service;

import com.financemanagerai.expense_service.entity.Expense;
import com.financemanagerai.expense_service.entity.ExpenseCategory;
import com.financemanagerai.expense_service.repository.ExpenseCategoryRepository;
import com.financemanagerai.expense_service.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          ExpenseCategoryRepository categoryRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
    }

    // Add expense (must belong to active category visible to requester)
    public Expense addExpense(Expense expense, Long categoryId, String requester, boolean isAdmin) {
        ExpenseCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.isActive()) {
            throw new RuntimeException("Cannot add expense to inactive category");
        }
        if (!category.isGlobal() && !category.getCreatedBy().equals(requester)) {
            throw new RuntimeException("You can only use your own categories");
        }

        // Optional duplicate check
        List<Expense> duplicates = expenseRepository.findByUsernameAndDateBetween(
                requester, expense.getDate(), expense.getDate());
        boolean duplicateExists = duplicates.stream()
                .anyMatch(e -> e.getCategory().equals(category) &&
                        e.getAmount().equals(expense.getAmount()) &&
                        e.getDescription().equals(expense.getDescription()));
        if (duplicateExists) {
            throw new RuntimeException("Duplicate expense detected");
        }

        expense.setCategory(category);
        expense.setUsername(requester);
        expense.setActive(true); // soft delete flag
        return expenseRepository.save(expense);
    }

    // List expenses for a user (active only or include inactive)
    public List<Expense> getExpensesForUser(String username, boolean includeInactive) {
        List<Expense> expenses = expenseRepository.findByUsername(username);
        return includeInactive ? expenses : expenses.stream().filter(Expense::isActive).toList();
    }

    // Soft delete expense (only owner or admin)
    public void deactivateExpense(Long expenseId, String requester, boolean isAdmin) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!isAdmin && !expense.getUsername().equals(requester)) {
            throw new RuntimeException("Not allowed to delete this expense");
        }

        expense.setActive(false);
        expenseRepository.save(expense);
    }
}