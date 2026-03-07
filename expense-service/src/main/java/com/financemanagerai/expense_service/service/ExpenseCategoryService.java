package com.financemanagerai.expense_service.service;

import com.financemanagerai.expense_service.entity.ExpenseCategory;
import com.financemanagerai.expense_service.exception.CategoryAlreadyExistsException;
import com.financemanagerai.expense_service.exception.ResourceNotFoundException;
import com.financemanagerai.expense_service.exception.UnauthorizedException;
import com.financemanagerai.expense_service.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Automatically creates constructor for final fields
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;

    /**
     * Creates a new category or links a subcategory to a parent.
     */
    @Transactional
    public ExpenseCategory createCategory(ExpenseCategory category, Long parentId, String requester, boolean isAdmin) {
        if (category.isGlobal() && !isAdmin) {
            throw new UnauthorizedException("Only admins can create global categories");
        }

        // Handle Hierarchy Link
        if (parentId != null) {
            ExpenseCategory parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentId));
            category.setParent(parent);
        }

        // Duplicate Check
        categoryRepository.findByNameAndActiveTrue(category.getName())
                .ifPresent(existing -> {
                    throw new CategoryAlreadyExistsException("Category with this name already exists and is active");
                });

        category.setCreatedBy(requester);
        category.setUpdatedBy(requester);
        category.setActive(true);

        return categoryRepository.save(category);
    }

    /**
     * Fixes the "cannot find symbol" error.
     * Returns a flat list of all categories the user has access to.
     */
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

    /**
     * Returns only Top-Level (Root) categories for the hierarchical Tree View.
     */
    public List<ExpenseCategory> listRootCategoriesForUser(String username, boolean includeInactive) {
        List<ExpenseCategory> all = listCategoriesForUser(username, includeInactive);

        // Filter for categories that have no parent (the roots)
        return all.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.toList());
    }

    /**
     * Soft delete/deactivate a category and all its children.
     */
    @Transactional
    public void deactivateCategory(Long id, String requester, boolean isAdmin) {
        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (category.isGlobal() && !isAdmin) {
            throw new UnauthorizedException("Only admins can deactivate global categories");
        }

        if (!isAdmin && !category.getCreatedBy().equals(requester)) {
            throw new UnauthorizedException("Not allowed to delete this category");
        }

        deactivateRecursive(category, requester);
    }

    private void deactivateRecursive(ExpenseCategory category, String requester) {
        category.setActive(false);
        category.setUpdatedBy(requester);

        if (category.getSubcategories() != null) {
            for (ExpenseCategory sub : category.getSubcategories()) {
                deactivateRecursive(sub, requester);
            }
        }
        categoryRepository.save(category);
    }
}