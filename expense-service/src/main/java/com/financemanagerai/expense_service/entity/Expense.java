package com.financemanagerai.expense_service.entity;

import com.financemanagerai.expense_service.entity.enums.Currency;
import com.financemanagerai.expense_service.entity.enums.PaymentMethod;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double amount;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    @Size(max = 255)
    private String description; // notes like "Dinner with client"

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod; // CASH, CARD, UPI, etc.

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency = Currency.INR; // default rupee if not provided

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String username; // from JWT

    // Optional: many-to-many tags
    @ElementCollection
    @CollectionTable(name = "expense_tags", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "tag", length = 30)
    private Set<String> tags = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;
}