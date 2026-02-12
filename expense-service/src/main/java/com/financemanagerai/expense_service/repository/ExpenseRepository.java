package com.financemanagerai.expense_service.repository;

import com.financemanagerai.expense_service.entity.Expense;
import com.financemanagerai.expense_service.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Find all expenses for a user
    List<Expense> findByUsername(String username);

    // Find expenses for a user within a date range
    List<Expense> findByUsernameAndDateBetween(String username, LocalDate startDate, LocalDate endDate);

    // Find expenses by category
    List<Expense> findByCategoryAndUsername(ExpenseCategory category, String username);

    // Find expenses by tag
    List<Expense> findByTagsContainingAndUsername(String tag, String username);
}