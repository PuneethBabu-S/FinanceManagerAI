package com.financemanagerai.expense_service.dto;

import com.financemanagerai.expense_service.entity.ExpenseCategory;
import lombok.*;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExpenseCategoryResponseDTO from(ExpenseCategory category) {
        return ExpenseCategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .isGlobal(category.isGlobal())
                .active(category.isActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}