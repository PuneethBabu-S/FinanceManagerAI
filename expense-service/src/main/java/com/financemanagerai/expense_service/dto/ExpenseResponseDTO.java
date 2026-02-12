package com.financemanagerai.expense_service.dto;

import com.financemanagerai.expense_service.entity.Expense;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponseDTO {

    private Long id;
    private Double amount;
    private LocalDate date;
    private String description;
    private String paymentMethod;
    private String currency;
    private String categoryName;
    private String username;
    private Set<String> tags;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExpenseResponseDTO from(Expense expense) {
        return ExpenseResponseDTO.builder()
                .id(expense.getId())
                .amount(expense.getAmount())
                .date(expense.getDate())
                .description(expense.getDescription())
                .paymentMethod(expense.getPaymentMethod().name())
                .currency(expense.getCurrency().name())
                .categoryName(expense.getCategory().getName())
                .username(expense.getUsername())
                .tags(expense.getTags())
                .active(expense.isActive())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}