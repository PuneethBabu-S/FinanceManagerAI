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
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    @PostMapping
    public ExpenseCategoryResponseDTO createCategory(@RequestBody ExpenseCategoryRequestDTO request,
                                                     Authentication authentication) {
        String requester = getRequester(authentication);
        boolean admin = isAdmin(authentication);

        ExpenseCategory category = request.toEntity();
        ExpenseCategory saved = categoryService.createCategory(category, requester, admin);

        return ExpenseCategoryResponseDTO.from(saved);
    }

    @GetMapping
    public List<ExpenseCategoryResponseDTO> listCategories(Authentication authentication,
                                                           @RequestParam(defaultValue = "false") boolean includeInactive) {
        String requester = getRequester(authentication);
        List<ExpenseCategory> categories = categoryService.listCategoriesForUser(requester, includeInactive);

        return categories.stream().map(ExpenseCategoryResponseDTO::from).toList();
    }

    @DeleteMapping("/{id}")
    public void deactivateCategory(@PathVariable Long id,
                                   Authentication authentication) {
        String requester = getRequester(authentication);
        boolean admin = isAdmin(authentication);
        categoryService.deactivateCategory(id, requester, admin);
    }
}