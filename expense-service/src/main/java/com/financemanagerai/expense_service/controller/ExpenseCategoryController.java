package com.financemanagerai.expense_service.controller;

import com.financemanagerai.expense_service.dto.ExpenseCategoryRequestDTO;
import com.financemanagerai.expense_service.dto.ExpenseCategoryResponseDTO;
import com.financemanagerai.expense_service.entity.ExpenseCategory;
import com.financemanagerai.expense_service.service.ExpenseCategoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryService categoryService;

    public ExpenseCategoryController(ExpenseCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    private String getRequester(Authentication authentication) {
        return authentication.getName();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ADMIN"));
    }

    @PostMapping
    public ExpenseCategoryResponseDTO createCategory(@RequestBody ExpenseCategoryRequestDTO request,
                                                     Authentication authentication) {
        String requester = getRequester(authentication);
        boolean admin = isAdmin(authentication);

        ExpenseCategory category = request.toEntity();

        ExpenseCategory saved = categoryService.createCategory(
                category,
                request.getParentId(),
                requester,
                admin
        );

        return ExpenseCategoryResponseDTO.from(saved);
    }

    @GetMapping
    public List<ExpenseCategoryResponseDTO> listCategories(Authentication authentication,
                                                           @RequestParam(defaultValue = "false") boolean includeInactive,
                                                           @RequestParam(defaultValue = "false") boolean treeView) {
        String requester = getRequester(authentication);

        if (treeView) {
            List<ExpenseCategory> rootCategories = categoryService.listRootCategoriesForUser(requester, includeInactive);
            return rootCategories.stream().map(ExpenseCategoryResponseDTO::from).toList();
        }

        // Standard flat list fetch
        List<ExpenseCategory> categories = categoryService.listCategoriesForUser(requester, includeInactive);
        return categories.stream().map(ExpenseCategoryResponseDTO::from).toList();
    }

    @PutMapping("/{id}")
    public ExpenseCategoryResponseDTO updateCategory(@PathVariable Long id,
                                                     @RequestBody ExpenseCategoryRequestDTO request,
                                                     Authentication authentication) {
        ExpenseCategory updated = categoryService.updateCategory(
                id,
                request.getName(),
                request.getDescription(),
                request.getParentId(),
                getRequester(authentication),
                isAdmin(authentication)
        );
        return ExpenseCategoryResponseDTO.from(updated);
    }

    /**
     * Reactivate an inactive category.
     * Uses @PatchMapping as we are only updating the 'active' state.
     */
    @PatchMapping("/{id}/reactivate")
    public ExpenseCategoryResponseDTO reactivateCategory(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "false") boolean recursive,
                                                         Authentication authentication) {
        ExpenseCategory reactivated = categoryService.reactivateCategory(
                id,
                getRequester(authentication),
                isAdmin(authentication),
                recursive
        );
        return ExpenseCategoryResponseDTO.from(reactivated);
    }

    @DeleteMapping("/{id}")
    public void deactivateCategory(@PathVariable Long id,
                                   Authentication authentication) {
        String requester = getRequester(authentication);
        boolean admin = isAdmin(authentication);
        categoryService.deactivateCategory(id, requester, admin);
    }
}