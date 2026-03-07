package com.financemanagerai.expense_service.dto;

import com.financemanagerai.expense_service.entity.ExpenseCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseCategoryResponseDTO {

    private Long id;
    private String name;
    private String description;
    private boolean isGlobal;
    private boolean active;
    private Long parentId; // Useful for the frontend to know the relationship
    private List<ExpenseCategoryResponseDTO> subcategories; // The "Base Structure" support
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExpenseCategoryResponseDTO from(ExpenseCategory category) {
        if (category == null) return null;

        return ExpenseCategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .isGlobal(category.isGlobal())
                .active(category.isActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                // Set the parent ID from the entity
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                // RECURSION: Map children entities to child DTOs
                .subcategories(category.getSubcategories() != null ?
                        category.getSubcategories().stream()
                                .map(ExpenseCategoryResponseDTO::from)
                                .collect(Collectors.toList()) : null)
                .build();
    }
}