package com.financemanagerai.expense_service.repository;

import com.financemanagerai.expense_service.entity.ExpenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    // Find active categories by name
    Optional<ExpenseCategory> findByNameAndActiveTrue(String name);

    // Find all active categories
    List<ExpenseCategory> findAllByActiveTrue();

    // Find subcategories under a parent
    List<ExpenseCategory> findByParentAndActiveTrue(ExpenseCategory parent);

    // Find categories created by a specific user
    List<ExpenseCategory> findByCreatedByAndActiveTrue(String createdBy);


    Optional<ExpenseCategory> findByNameAndActiveFalse(String name);

    List<ExpenseCategory> findByCreatedBy(String username);

    List<ExpenseCategory> findByIsGlobalTrue();

    List<ExpenseCategory> findByIsGlobalTrueAndActiveTrue();
}