package com.financemanagerai.expense_service.dto;

import com.financemanagerai.expense_service.entity.Expense;
import com.financemanagerai.expense_service.entity.enums.Currency;
import com.financemanagerai.expense_service.entity.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequestDTO {

    @NotNull
    @Positive
    private Double amount;

    @NotNull
    private LocalDate date;

    @Size(max = 255)
    private String description;

    @NotBlank
    private String paymentMethod; // CASH, CARD, UPI, etc.

    @NotBlank
    private String currency; // INR, USD, etc.

    @NotBlank
    private Long categoryId;

    private Set<String> tags;

    public Expense toEntity() {
        return Expense.builder()
                .amount(amount)
                .date(date)
                .description(description)
                .paymentMethod(PaymentMethod.valueOf(paymentMethod))
                .currency(Currency.valueOf(currency))
                .tags(tags)
                .active(true)
                .build();
    }
}