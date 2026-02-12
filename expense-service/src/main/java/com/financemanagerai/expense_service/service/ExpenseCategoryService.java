package com.financemanagerai.expense_service.service;

import com.financemanagerai.expense_service.entity.ExpenseCategory;
import com.financemanagerai.expense_service.repository.ExpenseCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;

    public ExpenseCategoryService(ExpenseCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Create category (admin can create global, user can create personal)
    public ExpenseCategory createCategory(ExpenseCategory category, String requester, boolean isAdmin) {
        if (category.isGlobal() && !isAdmin) {
            throw new RuntimeException("Only admins can create global categories");
        }

        // Check for duplicate active category
        categoryRepository.findByNameAndActiveTrue(category.getName())
                .ifPresent(existing -> {
                    throw new RuntimeException("Category with this name already exists and is active");
                });

        // If inactive category exists with same name, reactivate instead of creating new
        return categoryRepository.findByNameAndActiveFalse(category.getName())
                .map(inactive -> {
                    inactive.setActive(true);
                    inactive.setDescription(category.getDescription());
                    inactive.setGlobal(category.isGlobal());
                    inactive.setUpdatedBy(requester); // requester always recorded
                    return categoryRepository.save(inactive);
                })
                .orElseGet(() -> {
                    category.setCreatedBy(requester);
                    category.setUpdatedBy(requester);
                    category.setActive(true);
                    return categoryRepository.save(category);
                });
    }

    // List categories visible to a user (global + personal)
    public List<ExpenseCategory> listCategoriesForUser(String username, boolean includeInactive) {
        List<ExpenseCategory> globalCategories = includeInactive
                ? categoryRepository.findByIsGlobalTrue()
                : categoryRepository.findByIsGlobalTrueAndActiveTrue();

        List<ExpenseCategory> userCategories = includeInactive
                ? categoryRepository.findByCreatedBy(username)
                : categoryRepository.findByCreatedByAndActiveTrue(username);

        List<ExpenseCategory> allVisible = new ArrayList<>();
        allVisible.addAll(globalCategories);
        allVisible.addAll(userCategories);
        return allVisible;
    }

    // Soft delete category (only owner or admin)
    public void deactivateCategory(Long id, String requester, boolean isAdmin) {
        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.isGlobal() && !isAdmin) {
            throw new RuntimeException("Only admins can deactivate global categories");
        }

        if (!isAdmin && !category.getCreatedBy().equals(requester)) {
            throw new RuntimeException("Not allowed to delete this category");
        }

        category.setActive(false);
        category.setUpdatedBy(requester); // requester always recorded
        categoryRepository.save(category);
    }
}