package com.financemanagerai.expense_service.dto;

import com.financemanagerai.expense_service.entity.ExpenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseCategoryRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    private boolean isGlobal;

    private Long parentId;

    public ExpenseCategory toEntity() {
        return ExpenseCategory.builder()
                .name(name)
                .description(description)
                .isGlobal(isGlobal)
                .active(true)
                .build();
    }
}